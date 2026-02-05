import logging
from typing import List, Dict, Any
from langchain_google_genai import GoogleGenerativeAIEmbeddings, ChatGoogleGenerativeAI
from langchain_core.messages import HumanMessage
from app.settings import get_settings
from app.clients.event_client import event_service_client
from app.clients.redis_client import redis_cache_client
import warnings


warnings.filterwarnings("ignore", category=FutureWarning, module="langchain_google_genai")


settings = get_settings()
logger = logging.getLogger(__name__)



class AIService:
    """
    AI Service with 3-Tier Survival Mode Fallback Strategy
    
    TIER 1: AI Generation with 3-Key Rotation (gemini-2.5-flash-lite ONLY)
    TIER 2: Redis Cache Fallback (Latest Events)
    TIER 3: HTTP Direct Call to Event Service
    """


    def __init__(self):
        # Initialize embedding models with all available API keys
        self.embedding_models = self._initialize_embedding_models()
        
        # STANDARDIZED MODEL: ONLY gemini-2.5-flash-lite
        self.ai_api_keys = [
            settings.GEMINI_API_KEY_1,
            settings.GEMINI_API_KEY_2,
            settings.GEMINI_API_KEY_3
        ]
        
        logger.info(f"✅ AIService initialized with gemini-2.5-flash-lite and {len(self.ai_api_keys)}-key rotation")


    def _initialize_embedding_models(self) -> List[tuple[str, GoogleGenerativeAIEmbeddings]]:
        """Initialize embedding models with all available API keys"""
        api_keys = settings.get_all_api_keys()
        models = []
        
        for idx, api_key in enumerate(api_keys, start=1):
            try:
                # Initialize with dimension parameter
                model = GoogleGenerativeAIEmbeddings(
                    model=settings.EMBEDDING_MODEL,
                    google_api_key=api_key,
                    task_type="retrieval_document",
                    # Truncate embeddings to 768 dimensions
                    dimensions=768
                )
                tier_name = f"Tier {idx}"
                models.append((tier_name, model))
                logger.info(f"✅ Initialized embedding model for {tier_name} (768 dimensions)")
            except Exception as e:
                logger.warning(f"⚠️ Failed to initialize embedding model for API Key {idx}: {e}")
        
        if not models:
            raise RuntimeError("❌ No valid API keys available for embedding models!")
        
        return models


    async def generate_embedding(self, text: str) -> List[float]:
        """
        Generate embedding with automatic API key fallback.
        Tries all API keys in sequence until one succeeds.
        Returns 768-dimension embeddings.
        """
        last_error = None
        
        for tier_name, embedding_model in self.embedding_models:
            try:
                logger.debug(f"🔄 [{tier_name}] Attempting embedding generation")
                embedding = await embedding_model.aembed_query(text)
                
                # Verify dimension
                if len(embedding) != 768:
                    logger.warning(f"⚠️ [{tier_name}] Unexpected dimension: {len(embedding)}, truncating to 768")
                    embedding = embedding[:768]
                
                logger.info(f"✅ [{tier_name}] Generated embedding (dim: {len(embedding)})")
                return embedding
                
            except Exception as error:
                error_msg = str(error)
                last_error = error
                
                is_auth_error = any(keyword in error_msg for keyword in [
                    "API_KEY_INVALID", "API key expired", "401", "403", "PERMISSION_DENIED"
                ])
                
                if is_auth_error:
                    logger.warning(f"⚠️ [{tier_name}] API key failed: {error_msg[:150]}")
                else:
                    logger.error(f"❌ [{tier_name}] Embedding failed: {error_msg[:150]}")
        
        logger.critical(f"🚨 ALL {len(self.embedding_models)} API keys failed!")
        raise Exception(f"All embedding API keys failed. Last error: {str(last_error)[:200]}")


    async def generate_recommendation_email(
        self,
        username: str,
        booked_event: dict,
        similar_events: List[dict],
        booked_event_id: int
    ) -> dict:
        """
        🎯 SURVIVAL MODE: 3-Tier Fallback Strategy
        
        TIER 1: AI Generation with 3-Key Rotation (gemini-2.5-flash-lite)
        TIER 2: Redis Cache Fallback
        TIER 3: HTTP Direct Call to Event Service
        
        Args:
            username: User's name
            booked_event: The event user just booked
            similar_events: Vector-searched similar events
            booked_event_id: ID of the booked event (for filtering)
        """
        
        # ============================================
        # TIER 1: AI GENERATION WITH 3-KEY ROTATION
        # ============================================
        logger.info("🎯 TIER 1: Attempting AI Generation with 3-Key Rotation")
        
        for idx, api_key in enumerate(self.ai_api_keys, start=1):
            try:
                logger.info(f"🔑 TIER 1 - Key {idx}: Attempting gemini-2.5-flash-lite")
                
                # Initialize model with current API key
                chat_model = ChatGoogleGenerativeAI(
                    model="gemini-2.5-flash-lite",
                    google_api_key=api_key,
                    temperature=0.7
                )
                
                # Generate email content
                email_content = await self._generate_with_ai_model(
                    model=chat_model,
                    username=username,
                    booked_event=booked_event,
                    similar_events=similar_events,
                    tier=f"TIER 1 - Key {idx}"
                )
                
                logger.info(f"✅ TIER 1 - Key {idx}: SUCCESS")
                return email_content
                
            except Exception as e:
                logger.warning(f"⚠️ TIER 1 - Key {idx}: FAILED - {str(e)[:200]}")
                # Continue to next key
        
        logger.warning("❌ TIER 1: All 3 AI keys failed. Falling back to TIER 2 (Redis)")
        
        # ============================================
        # TIER 2: REDIS CACHE FALLBACK
        # ============================================
        try:
            logger.info("🎯 TIER 2: Attempting Redis Cache Fallback")
            
            # Fetch latest events from Redis
            cached_events = await redis_cache_client.get_latest_events(limit=5)
            
            if cached_events:
                # Filter out the booked event
                filtered_events = [
                    event for event in cached_events 
                    if event.get("id") != booked_event_id
                ]
                
                # Select top 3
                top_events = filtered_events[:3]
                
                if top_events:
                    email_body = self._build_template_email(
                        username=username,
                        booked_event=booked_event,
                        recommended_events=top_events,
                        source="Redis Cache"
                    )
                    
                    logger.info(f"✅ TIER 2: Redis Cache SUCCESS ({len(top_events)} events)")
                    return {
                        "subject": "Trending Events You Might Like!",
                        "body": email_body
                    }
            
            logger.warning("⚠️ TIER 2: Redis cache empty or insufficient events")
            
        except Exception as e:
            logger.error(f"❌ TIER 2: Redis failed - {str(e)[:200]}")
        
        # ============================================
        # TIER 3: HTTP DIRECT CALL TO EVENT SERVICE
        # ============================================
        logger.info("🎯 TIER 3: Attempting HTTP Direct Call to Event Service")
        
        try:
            # Direct HTTP call to Event Service
            latest_events = await event_service_client.get_latest_events(limit=5)
            
            # Filter out the booked event
            filtered_events = [
                event for event in latest_events 
                if event.get("id") != booked_event_id
            ]
            
            # Select top 3
            top_events = filtered_events[:3]
            
            if not top_events:
                logger.warning("⚠️ TIER 3: No events available after filtering")
                return self._generate_minimal_fallback(username, booked_event)
            
            email_body = self._build_template_email(
                username=username,
                booked_event=booked_event,
                recommended_events=top_events,
                source="Event Service"
            )
            
            logger.info(f"✅ TIER 3: Event Service SUCCESS ({len(top_events)} events)")
            return {
                "subject": "Trending Events You Might Like!",
                "body": email_body
            }
            
        except Exception as e:
            logger.error(f"❌ TIER 3: Event Service failed - {str(e)[:200]}")
            
            # Ultimate fallback - minimal email
            logger.warning("🚨 ALL TIERS FAILED - Using minimal fallback")
            return self._generate_minimal_fallback(username, booked_event)


    async def _generate_with_ai_model(
        self,
        model: ChatGoogleGenerativeAI,
        username: str,
        booked_event: dict,
        similar_events: List[dict],
        tier: str
    ) -> dict:
        """Core AI generation logic"""
        
        if not similar_events:
            logger.warning(f"[{tier}] No similar events provided")
            event_list = "Check out our latest events on TicketBlitz!"
        else:
            event_list = "\n".join([
                f"- {e['title']} ({e['category']}) at {e['location']} on {e['date']} - ${e['price']}"
                for e in similar_events
            ])
        
        prompt = f"""You are an enthusiastic event recommendation assistant for TicketBlitz.

The user {username} just booked "{booked_event['title']}" ({booked_event['category']}).

Based on their interest, recommend these similar upcoming events:
{event_list}

Write a friendly, personalized email body (3-4 sentences) that:
1. Congratulates them on their booking
2. Explains why these events match their taste
3. Encourages them to explore these options

Do NOT include:
- Subject line
- Greeting (we'll add "Hi {username}")
- Signature (we'll add the TicketBlitz team signature)

Just write the main email content."""

        try:
            response = await model.ainvoke([HumanMessage(content=prompt)])
            email_body = response.content.strip()
            
            full_body = f"Hi {username},\n\n{email_body}\n\nHappy exploring!\n\nThe TicketBlitz Team"
            
            logger.info(f"✅ [{tier}] Email generated successfully")
            
            return {
                "subject": "You might also like these events!",
                "body": full_body
            }
            
        except Exception as e:
            logger.error(f"❌ [{tier}] AI generation error: {e}")
            raise


    def _build_template_email(
        self,
        username: str,
        booked_event: dict,
        recommended_events: List[dict],
        source: str
    ) -> str:
        """Build email using predefined template (Tier 2 & 3)"""
        
        event_bullets = "\n".join([
            f"• {event['title']} - {event['location']} on {event.get('date', 'TBA')}"
            for event in recommended_events
        ])
        
        email_body = f"""Hi {username},

Thank you for booking {booked_event['title']}!

While our AI recommendation system is currently experiencing high demand, we wanted to share some trending events you might enjoy:

{event_bullets}

These are popular events from our platform that match your interests!

Happy exploring!

The TicketBlitz Team"""
        
        logger.info(f"📧 Template email built from {source}")
        return email_body


    def _generate_minimal_fallback(self, username: str, booked_event: dict) -> dict:
        """Ultimate fallback when all tiers fail"""
        return {
            "subject": "Thanks for your booking!",
            "body": f"""Hi {username},

Thank you for booking {booked_event['title']}! We hope you enjoy the event.

Happy exploring!

The TicketBlitz Team"""
        }


# Global instance
ai_service = AIService()
