import pytest
import asyncio
from unittest.mock import AsyncMock, MagicMock, patch
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from app.settings import Settings
from app.db import Base


@pytest.fixture(scope="session")
def event_loop():
    """Create event loop for async tests"""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture
def mock_settings():
    """Mock settings for tests"""
    return Settings(
        DB_HOST="localhost",
        DB_PORT=5438,
        DB_NAME="test_db",
        DB_USER="test_user",
        DB_PASSWORD="test_pass",
        KAFKA_BOOTSTRAP_SERVERS="localhost:9092",
        GEMINI_API_KEY="test_key",
        GEMINI_FALLBACK_API_KEY="test_fallback_key",
        GEMINI_TERTIARY_API_KEY_1="test_key_1",
        GEMINI_TERTIARY_API_KEY_2="test_key_2",
        GEMINI_TERTIARY_API_KEY_3="test_key_3",
        EVENT_SERVICE_HOST="localhost",
        EVENT_SERVICE_PORT=8082,
        OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4318"
    )


@pytest.fixture
async def async_db_session():
    """Create in-memory SQLite async session for testing"""
    engine = create_async_engine(
        "sqlite+aiosqlite:///:memory:",
        echo=False
    )
    
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    
    async_session = async_sessionmaker(
        engine, class_=AsyncSession, expire_on_commit=False
    )
    
    async with async_session() as session:
        yield session
    
    await engine.dispose()


@pytest.fixture
def mock_kafka_producer():
    """Mock Kafka producer"""
    producer = AsyncMock()
    producer.start = AsyncMock()
    producer.stop = AsyncMock()
    producer.send_and_wait = AsyncMock()
    return producer


@pytest.fixture
def mock_kafka_consumer():
    """Mock Kafka consumer"""
    consumer = AsyncMock()
    consumer.start = AsyncMock()
    consumer.stop = AsyncMock()
    return consumer


@pytest.fixture
def mock_embedding_model():
    """Mock embedding model"""
    model = AsyncMock()
    model.aembed_query = AsyncMock(return_value=[0.1] * 768)
    return model


@pytest.fixture
def mock_chat_model():
    """Mock chat model"""
    model = AsyncMock()
    response = MagicMock()
    response.content = "This is a test recommendation email."
    model.ainvoke = AsyncMock(return_value=response)
    return model
