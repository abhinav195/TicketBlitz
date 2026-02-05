import json
import logging
from typing import Dict, Any
from aiokafka import AIOKafkaProducer
from app.settings import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)


class KafkaProducerService:
    """Async Kafka Producer for sending emails"""
    
    def __init__(self):
        self.producer = None
    
    async def start(self):
        """Initialize the producer"""
        self.producer = AIOKafkaProducer(
            bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )
        await self.producer.start()
        logger.info("✅ Kafka Producer started")
    
    async def send_email_dispatch(self, email_data: Dict[str, Any]):
        """Send email dispatch event to notification service"""
        try:
            await self.producer.send_and_wait(
                settings.TOPIC_EMAIL_DISPATCH,
                value=email_data
            )
            logger.info(f"📤 Email dispatch sent to {email_data['recipientEmail']}")
        except Exception as e:
            logger.error(f"❌ Failed to send email dispatch: {e}")
            raise
    
    async def stop(self):
        """Gracefully stop the producer"""
        if self.producer:
            await self.producer.stop()


# Global instance
kafka_producer = KafkaProducerService()
