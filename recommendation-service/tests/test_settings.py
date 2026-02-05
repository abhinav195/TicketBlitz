import pytest
from app.settings import Settings, get_settings


class TestSettings:
    """Test Settings configuration"""
    
    def test_settings_initialization(self, mock_settings):
        """Test settings can be initialized"""
        assert mock_settings.DB_HOST == "localhost"
        assert mock_settings.DB_PORT == 5438
        assert mock_settings.KAFKA_BOOTSTRAP_SERVERS == "localhost:9092"
    
    def test_database_url_property(self, mock_settings):
        """Test database URL generation"""
        expected = "postgresql+asyncpg://test_user:test_pass@localhost:5438/test_db"
        assert mock_settings.database_url == expected
    
    def test_event_service_url_property(self, mock_settings):
        """Test event service URL generation"""
        expected = "http://localhost:8082"
        assert mock_settings.event_service_url == expected
    
    def test_get_all_api_keys(self, mock_settings):
        """Test all API keys are returned in order"""
        keys = mock_settings.get_all_api_keys()
        assert len(keys) == 5
        assert keys[0] == "test_key"
        assert keys[1] == "test_fallback_key"
        assert keys[2] == "test_key_1"
    
    def test_get_settings_cached(self):
        """Test get_settings returns cached instance"""
        settings1 = get_settings()
        settings2 = get_settings()
        assert settings1 is settings2
    
    def test_default_values(self):
        """Test default configuration values"""
        settings = Settings(
            GEMINI_API_KEY="test",
            GEMINI_FALLBACK_API_KEY="test",
            GEMINI_TERTIARY_API_KEY_1="test",
            GEMINI_TERTIARY_API_KEY_2="test",
            GEMINI_TERTIARY_API_KEY_3="test"
        )
        assert settings.EMBEDDING_DIMENSION == 768
        assert settings.SIMILARITY_TOP_K == 3
        assert settings.CIRCUIT_BREAKER_FAIL_THRESHOLD == 5
        assert settings.SERVICE_NAME == "recommendation-service"
