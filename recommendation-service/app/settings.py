from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # Database
    DB_HOST: str = "localhost"
    DB_PORT: int = 5438
    DB_NAME: str = "ticketblitz_recommendation"
    DB_USER: str = "postgres"
    DB_PASSWORD: str = "abhinav195"
    
    # Redis (Tier 2 Fallback)
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    
    # Kafka
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
    TOPIC_EVENTS_CREATED: str = "ticketblitz.events.created"
    TOPIC_RECOMMENDATION_REQUEST: str = "ticketblitz.recommendation.request"
    TOPIC_EMAIL_DISPATCH: str = "ticketblitz.email.dispatch"
    KAFKA_GROUP_ID: str = "recommendation-service"
    
    # Event Service (Tier 3 Fallback)
    EVENT_SERVICE_HOST: str = "localhost"
    EVENT_SERVICE_PORT: int = 8082
    EVENT_SERVICE_TIMEOUT: int = 10
    
    # Google Gemini API Keys (3-Key Rotation)
    GEMINI_API_KEY_1: str
    GEMINI_API_KEY_2: str
    GEMINI_API_KEY_3: str
    
    # Embedding API Keys (for embeddings)
    GEMINI_EMBEDDING_KEY_1: str = ""
    GEMINI_EMBEDDING_KEY_2: str = ""
    GEMINI_EMBEDDING_KEY_3: str = ""
    
    # Model Configuration
    EMBEDDING_MODEL: str = "models/gemini-embedding-001"
    CHAT_MODEL: str = "gemini-2.5-flash-lite"
    # Vector Configuration
    EMBEDDING_DIMENSION: int = 768
    
    # Vector Search
    SIMILARITY_TOP_K: int = 3
    
    # OpenTelemetry
    OTEL_SERVICE_NAME: str = "recommendation-service"
    OTEL_EXPORTER_OTLP_ENDPOINT: str = "http://localhost:4318"
    
    # Servicemodels/gemini-embedding-001
    SERVICE_NAME: str = "recommendation-service"
    
    @property
    def database_url(self) -> str:
        return f"postgresql+asyncpg://{self.DB_USER}:{self.DB_PASSWORD}@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"
    
    @property
    def event_service_url(self) -> str:
        return f"http://{self.EVENT_SERVICE_HOST}:{self.EVENT_SERVICE_PORT}"
    
    def get_all_api_keys(self) -> list:
        """Get all available API keys for embeddings"""
        keys = [self.GEMINI_API_KEY_1, self.GEMINI_API_KEY_2, self.GEMINI_API_KEY_3]
        if self.GEMINI_EMBEDDING_KEY_1:
            keys.append(self.GEMINI_EMBEDDING_KEY_1)
        if self.GEMINI_EMBEDDING_KEY_2:
            keys.append(self.GEMINI_EMBEDDING_KEY_2)
        if self.GEMINI_EMBEDDING_KEY_3:
            keys.append(self.GEMINI_EMBEDDING_KEY_3)
        return keys
    
    class Config:
        env_file = ".env"
        case_sensitive = True


@lru_cache()
def get_settings():
    return Settings()
