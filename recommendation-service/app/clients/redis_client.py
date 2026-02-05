import logging
import json
from typing import List, Dict, Any, Optional
import redis.asyncio as redis
from app.settings import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)


class RedisCacheClient:
    """Redis Client for Tier 2 Fallback"""
    
    def __init__(self):
        self.redis_host = settings.REDIS_HOST
        self.redis_port = settings.REDIS_PORT
        self.redis_client = None
    
    async def connect(self):
        """Initialize Redis connection"""
        try:
            self.redis_client = await redis.from_url(
                f"redis://{self.redis_host}:{self.redis_port}",
                encoding="utf-8",
                decode_responses=True
            )
            await self.redis_client.ping()
            logger.info("✅ Redis client connected successfully")
        except Exception as e:
            logger.error(f"❌ Failed to connect to Redis: {e}")
            self.redis_client = None
    
    async def get_latest_events(self, limit: int = 5) -> List[Dict[str, Any]]:
        """
        Fetch latest events from Redis cache.
        Used as Tier 2 fallback.
        
        Returns:
            List of event dictionaries
        """
        if not self.redis_client:
            await self.connect()
        
        if not self.redis_client:
            raise Exception("Redis client not available")
        
        try:
            logger.info(f"🔍 Fetching latest {limit} events from Redis")
            
            # Fetch events from Redis sorted set (assuming events are cached with scores)
            event_keys = await self.redis_client.zrevrange(
                "events:latest", 
                0, 
                limit - 1
            )
            
            if not event_keys:
                logger.warning("⚠️ No events found in Redis cache")
                return []
            
            # Fetch event details
            events = []
            for event_key in event_keys:
                event_data = await self.redis_client.get(f"event:{event_key}")
                if event_data:
                    events.append(json.loads(event_data))
            
            logger.info(f"✅ Retrieved {len(events)} events from Redis")
            return events
            
        except Exception as e:
            logger.error(f"❌ Failed to fetch events from Redis: {e}")
            raise
    
    async def close(self):
        """Close Redis connection"""
        if self.redis_client:
            await self.redis_client.close()
            logger.info("🔌 Redis connection closed")


# Singleton instance
redis_cache_client = RedisCacheClient()
