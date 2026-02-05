"""
Kafka messaging infrastructure.
Consumers and producers for event-driven architecture.
"""

from .consumers import event_ingestion_consumer, recommendation_request_consumer
from .producer import kafka_producer

__all__ = [
    "event_ingestion_consumer",
    "recommendation_request_consumer",
    "kafka_producer"
]
