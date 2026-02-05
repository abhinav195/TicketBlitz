import pytest
from unittest.mock import patch, AsyncMock, MagicMock
from app.services.ai_service import AIService
from pybreaker import CircuitBreakerError


class TestAIService:
    """Test AI Service with multi-tier fallback"""
    
    @pytest.mark.asyncio
    async def test_generate_embedding_success(self, mock_settings, mock_embedding_model):
        """Test successful embedding generation"""
        with patch("app.services.ai_service.settings", mock_settings):
            with patch("app.services.ai_service.GoogleGenerativeAIEmbeddings", return_value=mock_embedding_model):
                ai_service = AIService()
                
                embedding = await ai_service.generate_embedding("test text")
                
                assert len(embedding) == 768
                assert all(isinstance(x, float) for x in embedding)
    
    @pytest.mark.asyncio
    async def test_generate_embedding_fallback(self, mock_settings):
        """Test embedding fallback to secondary key"""
        with patch("app.services.ai_service.settings", mock_settings):
            mock_model_1 = AsyncMock()
            mock_model_1.aembed_query.side_effect = Exception("API_KEY_INVALID")
            
            mock_model_2 = AsyncMock()
            mock_model_2.aembed_query.return_value = [0.2] * 768
            
            with patch("app.services.ai_service.GoogleGenerativeAIEmbeddings", side_effect=[mock_model_1, mock_model_2, AsyncMock(), AsyncMock(), AsyncMock()]):
                ai_service = AIService()
                
                embedding = await ai_service.generate_embedding("test")
                
                assert len(embedding) == 768
    
    @pytest.mark.asyncio
    async def test_generate_recommendation_email_primary(self, mock_settings, mock_chat_model):
        """Test email generation with primary model"""
        with patch("app.services.ai_service.settings", mock_settings):
            with patch("app.services.ai_service.GoogleGenerativeAIEmbeddings", return_value=AsyncMock()):
                with patch("app.services.ai_service.ChatGoogleGenerativeAI", return_value=mock_chat_model):
                    ai_service = AIService()
                    
                    booked_event = {"title": "Concert", "category": "Music", "location": "NY", "price": "50", "date": "2026-02-01"}
                    similar_events = [{"title": "Festival", "category": "Music", "location": "LA", "price": "100", "date": "2026-03-01"}]
                    
                    result = await ai_service.generate_recommendation_email(
                        username="John",
                        booked_event=booked_event,
                        similar_events=similar_events,
                        booked_event_id=1
                    )
                    
                    assert "subject" in result
                    assert "body" in result
                    assert "John" in result["body"]
    
    @pytest.mark.asyncio
    async def test_generate_static_fallback(self, mock_settings):
        """Test TIER 6 static fallback"""
        with patch("app.services.ai_service.settings", mock_settings):
            with patch("app.services.ai_service.GoogleGenerativeAIEmbeddings", return_value=AsyncMock()):
                with patch("app.services.ai_service.ChatGoogleGenerativeAI", return_value=AsyncMock()):
                    with patch("app.services.ai_service.event_service_client") as mock_event_client:
                        mock_event_client.get_latest_events = AsyncMock(return_value=[
                            {"id": 2, "title": "Event 2", "location": "NYC", "date": "2026-02-01"}
                        ])
                        
                        ai_service = AIService()
                        
                        booked_event = {"title": "Concert", "category": "Music", "location": "NY", "price": "50", "date": "2026-02-01"}
                        
                        result = await ai_service._generate_static_fallback("John", booked_event, 1)
                        
                        assert "subject" in result
                        assert "body" in result
                        assert "John" in result["body"]
