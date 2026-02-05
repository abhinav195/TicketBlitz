import logging
from typing import List, Dict, Any
import httpx
from app.settings import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)


class EventServiceClient:
    """Async HTTP Client for Event Service (Tier 3 Fallback)"""
    
    def __init__(self):
        self.base_url = settings.event_service_url
        self.timeout = settings.EVENT_SERVICE_TIMEOUT
    
    async def get_latest_events(self, limit: int = 5) -> List[Dict[str, Any]]:
        """
        Fetch latest events from Event Service.
        Called as Tier 3 fallback when AI models fail.
        
        Returns:
            List of event dictionaries
        """
        url = f"{self.base_url}/events/latest"
        params = {"limit": limit}
        
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                logger.info(f"🔗 Calling Event Service: {url}?limit={limit}")
                response = await client.get(url, params=params)
                response.raise_for_status()
                
                events = response.json()
                logger.info(f"✅ Retrieved {len(events)} latest events from Event Service")
                return events
                
        except httpx.TimeoutException:
            logger.error(f"⏱️ Timeout calling Event Service after {self.timeout}s")
            raise
        except httpx.HTTPStatusError as e:
            logger.error(f"❌ Event Service returned error: {e.response.status_code}")
            raise
        except Exception as e:
            logger.error(f"❌ Failed to call Event Service: {e}")
            raise


# Singleton instance
event_service_client = EventServiceClient()
