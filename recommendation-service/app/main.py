import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.kafka.consumers import event_ingestion_consumer, recommendation_request_consumer
from app.kafka.producer import kafka_producer
from app.services.telemetry import setup_telemetry, instrument_fastapi, instrument_sqlalchemy
from app.db import engine
from app.clients.redis_client import redis_cache_client
from app.settings import get_settings

settings = get_settings()
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifecycle manager"""
    logger.info("🚀 Starting Recommendation Service...")
    
    # Initialize OpenTelemetry
    setup_telemetry()
    instrument_sqlalchemy(engine)
    
    # Initialize Redis client
    try:
        await redis_cache_client.connect()
        logger.info("✅ Redis client initialized")
    except Exception as e:
        logger.warning(f"⚠️ Redis initialization failed (Tier 2 will be skipped): {e}")
    
    # Start Kafka components
    await kafka_producer.start()
    
    # Start consumers in background
    asyncio.create_task(event_ingestion_consumer.start())
    asyncio.create_task(recommendation_request_consumer.start())
    
    logger.info("✅ All services started successfully")
    
    yield
    
    # Shutdown
    logger.info("🛑 Shutting down Recommendation Service...")
    await event_ingestion_consumer.stop()
    await recommendation_request_consumer.stop()
    await kafka_producer.stop()
    await redis_cache_client.close()
    logger.info("✅ Graceful shutdown complete")


app = FastAPI(
    title="TicketBlitz Recommendation Service",
    version="2.0.0",
    description="AI-Powered Event Recommendations with 3-Tier Survival Mode",
    lifespan=lifespan
)

# Instrument FastAPI after app creation
instrument_fastapi(app)


@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "recommendation-service",
        "version": "2.0.0",
        "survival_mode": "3-Tier (AI → Redis → HTTP)"
    }


@app.get("/")
async def root():
    return {
        "message": "TicketBlitz Recommendation Service",
        "version": "2.0.0",
        "architecture": "3-Tier Survival Mode",
        "tiers": {
            "tier_1": "AI Generation (3-Key Rotation)",
            "tier_2": "Redis Cache Fallback",
            "tier_3": "HTTP Event Service Direct Call"
        }
    }
