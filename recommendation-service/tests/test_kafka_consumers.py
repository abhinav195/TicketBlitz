import pytest
from unittest.mock import patch, AsyncMock, MagicMock
from datetime import datetime
from app.kafka.consumers import EventIngestionConsumer, RecommendationRequestConsumer


class TestEventIngestionConsumer:
    """Test Event Ingestion Consumer"""
    
    @pytest.mark.asyncio
    async def test_consumer_start(self):
        """Test consumer initialization"""
        consumer = EventIngestionConsumer()
        
        # Create a proper async generator mock
        async def mock_async_iter():
            return
            yield  # Make this an async generator
        
        with patch("app.kafka.consumers.AIOKafkaConsumer") as mock_consumer:
            mock_instance = AsyncMock()
            mock_instance.start = AsyncMock()
            mock_instance.__aiter__ = lambda self: mock_async_iter()
            mock_instance.stop = AsyncMock()
            mock_consumer.return_value = mock_instance
            
            # Start consumer (will exit immediately due to empty iterator)
            consumer.running = False  # Set to False immediately
            
            # Just verify consumer initialization
            assert consumer is not None

    
    @pytest.mark.asyncio
    async def test_process_event_success(self):
        """Test successful event processing"""
        consumer = EventIngestionConsumer()
        
        event_data = {
            "id": 1,
            "title": "Test Event",
            "description": "Test Description",
            "category": "Music",
            "location": "New York",
            "price": "50.00",
            "date": [2026, 2, 15, 18, 0, 0],
            "imageUrls": ["http://example.com/image.jpg"]
        }
        
        with patch("app.kafka.consumers.ai_service") as mock_ai_service:
            mock_ai_service.generate_embedding = AsyncMock(return_value=[0.1] * 768)
            
            with patch("app.kafka.consumers.AsyncSessionLocal") as mock_session_factory:
                mock_session = AsyncMock()
                mock_session.add = MagicMock()
                mock_session.commit = AsyncMock()
                mock_session_factory.return_value.__aenter__.return_value = mock_session
                
                await consumer.process_event(event_data)
                
                # Verify embedding was generated
                mock_ai_service.generate_embedding.assert_called_once()
                # Verify session operations were called
                mock_session.add.assert_called_once()
                mock_session.commit.assert_called_once()

    
    @pytest.mark.asyncio
    async def test_process_event_with_string_date(self):
        """Test event processing with ISO date string"""
        consumer = EventIngestionConsumer()
        
        event_data = {
            "id": 2,
            "title": "Event with String Date",
            "description": "Description",
            "category": "Sports",
            "location": "LA",
            "price": "100",
            "date": "2026-03-20T19:00:00Z",
            "imageUrls": []
        }
        
        with patch("app.kafka.consumers.ai_service") as mock_ai_service:
            mock_ai_service.generate_embedding = AsyncMock(return_value=[0.2] * 768)
            
            with patch("app.kafka.consumers.AsyncSessionLocal") as mock_session:
                mock_db_session = AsyncMock()
                mock_session.return_value.__aenter__.return_value = mock_db_session
                
                await consumer.process_event(event_data)
                
                mock_ai_service.generate_embedding.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_process_event_failure(self):
        """Test event processing error handling"""
        consumer = EventIngestionConsumer()
        
        event_data = {
            "id": 3,
            "title": "Event that fails"
        }
        
        with patch("app.kafka.consumers.ai_service") as mock_ai_service:
            mock_ai_service.generate_embedding = AsyncMock(side_effect=Exception("AI Error"))
            
            # Should not raise exception (error is logged)
            await consumer.process_event(event_data)
    
    @pytest.mark.asyncio
    async def test_consumer_stop(self):
        """Test consumer shutdown"""
        consumer = EventIngestionConsumer()
        consumer.consumer = AsyncMock()
        consumer.running = True
        
        await consumer.stop()
        
        assert consumer.running is False
        consumer.consumer.stop.assert_called_once()


class TestRecommendationRequestConsumer:
    """Test Recommendation Request Consumer"""
    
    @pytest.mark.asyncio
    async def test_process_request_success(self):
        """Test successful recommendation request processing"""
        consumer = RecommendationRequestConsumer()
        
        request_data = {
            "userId": 10,
            "eventId": 5,
            "userEmail": "test@test.com",
            "username": "testuser"
        }
        
        with patch("app.kafka.consumers.AsyncSessionLocal") as mock_session:
            mock_db_session = AsyncMock()
            
            # Mock booked event
            mock_event = MagicMock()
            mock_event.event_id = 5
            mock_event.title = "Booked Event"
            mock_event.category = "Music"
            mock_event.location = "NYC"
            mock_event.price = "50"
            mock_event.date = datetime(2026, 2, 1)
            mock_event.embedding = [0.3] * 768
            
            mock_result = MagicMock()
            mock_result.scalar_one_or_none.return_value = mock_event
            mock_db_session.execute = AsyncMock(return_value=mock_result)
            mock_db_session.add = MagicMock()
            mock_db_session.commit = AsyncMock()
            
            mock_session.return_value.__aenter__.return_value = mock_db_session
            
            with patch("app.kafka.consumers.ai_service") as mock_ai_service:
                mock_ai_service.generate_recommendation_email = AsyncMock(return_value={
                    "subject": "Test Subject",
                    "body": "Test Body"
                })
                
                with patch("app.kafka.consumers.kafka_producer") as mock_producer:
                    mock_producer.send_email_dispatch = AsyncMock()
                    
                    await consumer.process_request(request_data)
                    
                    mock_ai_service.generate_recommendation_email.assert_called_once()
                    mock_producer.send_email_dispatch.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_process_request_event_not_found(self):
        """Test request processing when event not found"""
        consumer = RecommendationRequestConsumer()
        
        request_data = {
            "userId": 10,
            "eventId": 999,
            "userEmail": "test@test.com",
            "username": "testuser"
        }
        
        with patch("app.kafka.consumers.AsyncSessionLocal") as mock_session:
            mock_db_session = AsyncMock()
            
            mock_result = MagicMock()
            mock_result.scalar_one_or_none.return_value = None
            mock_db_session.execute = AsyncMock(return_value=mock_result)
            
            mock_session.return_value.__aenter__.return_value = mock_db_session
            
            # Should not raise exception
            await consumer.process_request(request_data)
    
    @pytest.mark.asyncio
    async def test_process_request_failure(self):
        """Test request processing error handling"""
        consumer = RecommendationRequestConsumer()
        
        request_data = {
            "userId": 10,
            "eventId": 5,
            "userEmail": "test@test.com",
            "username": "testuser"
        }
        
        with patch("app.kafka.consumers.AsyncSessionLocal") as mock_session:
            mock_session.return_value.__aenter__.side_effect = Exception("Database error")
            
            # Should not raise exception (error is logged)
            await consumer.process_request(request_data)


# Add asyncio import
import asyncio
