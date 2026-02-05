"""
Client modules for external service communication.
Supports Tier 2 (Redis) and Tier 3 (Event Service) fallbacks.
"""

from .event_client import event_service_client
from .redis_client import redis_cache_client

__all__ = [
    "event_service_client",
    "redis_cache_client"
]
