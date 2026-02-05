import pytest
from unittest.mock import patch, AsyncMock, MagicMock
import httpx
from app.clients.event_client import EventServiceClient


class TestEventServiceClient:
    """Test EventServiceClient"""
    
    @pytest.mark.asyncio
    async def test_get_latest_events_success(self, mock_settings):
        """Test successful event fetch"""
        client = EventServiceClient()
        client.base_url = mock_settings.event_service_url
        
        mock_events = [
            {"id": 1, "title": "Event 1"},
            {"id": 2, "title": "Event 2"}
        ]
        
        with patch("httpx.AsyncClient") as mock_client:
            mock_response = MagicMock()
            mock_response.json = lambda: mock_events  # FIX: Use lambda instead of AsyncMock
            mock_response.raise_for_status = MagicMock()
            
            mock_context = AsyncMock()
            mock_context.__aenter__.return_value.get = AsyncMock(return_value=mock_response)
            mock_client.return_value = mock_context
            
            events = await client.get_latest_events(limit=2)
            
            assert len(events) == 2
            assert events[0]["id"] == 1
    
    @pytest.mark.asyncio
    async def test_get_latest_events_timeout(self, mock_settings):
        """Test timeout error handling"""
        client = EventServiceClient()
        
        with patch("httpx.AsyncClient") as mock_client:
            mock_context = AsyncMock()
            mock_context.__aenter__.return_value.get = AsyncMock(
                side_effect=httpx.TimeoutException("Timeout")
            )
            mock_client.return_value = mock_context
            
            with pytest.raises(httpx.TimeoutException):
                await client.get_latest_events()
    
    @pytest.mark.asyncio
    async def test_get_latest_events_http_error(self, mock_settings):
        """Test HTTP error handling"""
        client = EventServiceClient()
        
        with patch("httpx.AsyncClient") as mock_client:
            mock_response = MagicMock()
            mock_response.status_code = 404
            mock_response.raise_for_status = MagicMock(side_effect=httpx.HTTPStatusError(
                "Not found", request=MagicMock(), response=mock_response
            ))
            
            mock_context = AsyncMock()
            mock_context.__aenter__.return_value.get = AsyncMock(return_value=mock_response)
            mock_client.return_value = mock_context
            
            with pytest.raises(httpx.HTTPStatusError):
                await client.get_latest_events()
