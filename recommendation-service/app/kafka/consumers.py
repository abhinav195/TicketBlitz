import asyncio
import json
import logging
from typing import Dict, Any
from datetime import datetime
from aiokafka import AIOKafkaConsumer
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession
from app.db import AsyncSessionLocal
from app.models import EventVector, UserHistory
from app.services.ai_service import ai_service
from app.kafka.producer import kafka_producer
from app.settings import get_settings
from opentelemetry import trace

settings = get_settings()
logger = logging.getLogger(__name__)
tracer = trace.get_tracer(__name__)


class EventIngestionConsumer:
    """Consumes events from ticketblitz.events.created"""
    
    def __init__(self):
        self.consumer = None
        self.running = False
    
    async def start(self):
        """Initialize and start the consumer"""
        self.consumer = AIOKafkaConsumer(
            settings.TOPIC_EVENTS_CREATED,
            bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
            group_id=f"{settings.KAFKA_GROUP_ID}-event-ingestion",
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
            auto_offset_reset='earliest',
            enable_auto_commit=True
        )
        
        await self.consumer.start()
        self.running = True
        logger.info(f"✅ EventIngestionConsumer started. Listening to {settings.TOPIC_EVENTS_CREATED}")
        
        try:
            async for message in self.consumer:
                if not self.running:
                    break
                await self.process_event(message.value)
        finally:
            await self.consumer.stop()
    
    async def process_event(self, event_data: Dict[str, Any]):
        """Process incoming event and store embedding"""
        with tracer.start_as_current_span("process_event_ingestion") as span:
            try:
                event_id = event_data.get('id')
                span.set_attribute("event.id", event_id)
                span.set_attribute("event.title", event_data.get('title', ''))
                
                logger.info(f"📚 Processing Event: {event_id} - {event_data.get('title')}")
                
                # Generate embedding text
                embed_text = f"{event_data['title']}. {event_data['description']}. Category: {event_data['category']}"
                
                with tracer.start_as_current_span("generate_embedding"):
                    embedding = await ai_service.generate_embedding(embed_text)
                
                # Convert date
                date_value = event_data['date']
                if isinstance(date_value, list):
                    event_datetime = datetime(*date_value)
                elif isinstance(date_value, str):
                    event_datetime = datetime.fromisoformat(date_value.replace('Z', '+00:00'))
                else:
                    event_datetime = date_value
                
                # Store in database
                with tracer.start_as_current_span("save_event_vector"):
                    async with AsyncSessionLocal() as session:
                        event_vector = EventVector(
                            event_id=event_data['id'],
                            title=event_data['title'],
                            description=event_data['description'],
                            category=event_data['category'],
                            location=event_data['location'],
                            price=str(event_data['price']),
                            date=event_datetime,
                            image_urls=event_data.get('imageUrls', []),
                            embedding=embedding
                        )
                        
                        session.add(event_vector)
                        await session.commit()
                        
                        logger.info(f"✅ Embedding stored for Event {event_data['id']}")
                        span.set_attribute("status", "success")
            
            except Exception as e:
                logger.error(f"❌ Failed to process event {event_data.get('id')}: {e}", exc_info=True)
                span.set_attribute("status", "error")
                span.record_exception(e)
    
    async def stop(self):
        """Gracefully stop the consumer"""
        self.running = False
        if self.consumer:
            await self.consumer.stop()


class RecommendationRequestConsumer:
    """Consumes recommendation requests from notification service"""
    
    def __init__(self):
        self.consumer = None
        self.running = False
    
    async def start(self):
        """Initialize and start the consumer"""
        self.consumer = AIOKafkaConsumer(
            settings.TOPIC_RECOMMENDATION_REQUEST,
            bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
            group_id=f"{settings.KAFKA_GROUP_ID}-recommendation",
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
            auto_offset_reset='earliest',
            enable_auto_commit=True
        )
        
        await self.consumer.start()
        self.running = True
        logger.info(f"✅ RecommendationRequestConsumer started. Listening to {settings.TOPIC_RECOMMENDATION_REQUEST}")
        
        try:
            async for message in self.consumer:
                if not self.running:
                    break
                await self.process_request(message.value)
        finally:
            await self.consumer.stop()
    
    async def process_request(self, request_data: Dict[str, Any]):
        """Process recommendation request with distributed tracing"""
        with tracer.start_as_current_span("process_recommendation_request") as span:
            try:
                user_id = request_data['userId']
                event_id = request_data['eventId']
                email = request_data['userEmail']
                username = request_data['username']
                
                span.set_attribute("user.id", user_id)
                span.set_attribute("event.id", event_id)
                span.set_attribute("user.email", email)
                
                logger.info(f"🧠 Recommendation Request: User {user_id} | Event {event_id}")
                
                async with AsyncSessionLocal() as session:
                    # STEP 1: Get the booked event's embedding
                    with tracer.start_as_current_span("fetch_booked_event"):
                        result = await session.execute(
                            select(EventVector).where(EventVector.event_id == event_id)
                        )
                        booked_event = result.scalar_one_or_none()
                        
                        if not booked_event:
                            logger.warning(f"❌ Event {event_id} not found in vector DB. Skipping.")
                            span.set_attribute("status", "event_not_found")
                            return
                    
                    # STEP 2: Save to user history
                    with tracer.start_as_current_span("save_user_history"):
                        user_history_entry = UserHistory(
                            user_id=user_id,
                            event_id=event_id,
                            username=username,
                            email=email,
                            event_embedding=booked_event.embedding
                        )
                        session.add(user_history_entry)
                        await session.commit()
                        logger.info(f"✅ User history updated for User {user_id}")
                    
                    # STEP 3: Find similar events
                    with tracer.start_as_current_span("vector_similarity_search"):
                        similar_events_query = text("""
                            SELECT event_id, title, description, category, location, price, date, image_urls,
                                   1 - (embedding <=> :embedding) as similarity
                            FROM event_vectors
                            WHERE event_id != :event_id
                            ORDER BY embedding <=> :embedding
                            LIMIT :limit
                        """)
                        
                        embedding_value = booked_event.embedding
                        if hasattr(embedding_value, 'tolist'):
                            embedding_value = embedding_value.tolist()
                        elif not isinstance(embedding_value, list):
                            embedding_value = list(embedding_value)
                        
                        embedding_str = '[' + ','.join(str(x) for x in embedding_value) + ']'
                        
                        result = await session.execute(
                            similar_events_query,
                            {
                                "embedding": embedding_str,
                                "event_id": event_id,
                                "limit": settings.SIMILARITY_TOP_K
                            }
                        )
                        similar_events = result.fetchall()
                        
                        similar_events_list = [
                            {
                                "event_id": row[0],
                                "title": row[1],
                                "description": row[2],
                                "category": row[3],
                                "location": row[4],
                                "price": row[5],
                                "date": str(row[6]),
                                "image_urls": row[7]
                            }
                            for row in similar_events
                        ] if similar_events else []
                        
                        span.set_attribute("similar_events.count", len(similar_events_list))
                        logger.info(f"🔍 Found {len(similar_events_list)} similar events")
                    
                    # STEP 4: Generate AI email with 3-Tier Fallback
                    booked_event_dict = {
                        "title": booked_event.title,
                        "category": booked_event.category,
                        "location": booked_event.location,
                        "price": booked_event.price,
                        "date": str(booked_event.date)
                    }
                    
                    with tracer.start_as_current_span("generate_recommendation_email"):
                        email_content = await ai_service.generate_recommendation_email(
                            username=username,
                            booked_event=booked_event_dict,
                            similar_events=similar_events_list,
                            booked_event_id=event_id
                        )
                    
                    # STEP 5: Dispatch email
                    with tracer.start_as_current_span("send_email_dispatch"):
                        email_dispatch_event = {
                            "recipientEmail": email,
                            "subject": email_content["subject"],
                            "body": email_content["body"]
                        }
                        
                        await kafka_producer.send_email_dispatch(email_dispatch_event)
                        logger.info(f"📧 Email dispatch sent for {email}")
                        span.set_attribute("status", "success")
            
            except Exception as e:
                logger.error(f"❌ Failed to process recommendation request: {e}", exc_info=True)
                span.set_attribute("status", "error")
                span.record_exception(e)
    
    async def stop(self):
        """Gracefully stop the consumer"""
        self.running = False
        if self.consumer:
            await self.consumer.stop()


# Global instances
event_ingestion_consumer = EventIngestionConsumer()
recommendation_request_consumer = RecommendationRequestConsumer()
