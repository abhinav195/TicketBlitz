import pytest
from unittest.mock import patch, AsyncMock
from app.kafka.producer import KafkaProducerService


class TestKafkaProducerService:
    """Test Kafka Producer"""
    
    @pytest.mark.asyncio
    async def test_producer_start(self):
        """Test producer initialization"""
        producer_service = KafkaProducerService()
        
        with patch("app.kafka.producer.AIOKafkaProducer") as mock_producer:
            mock_instance = AsyncMock()
            mock_producer.return_value = mock_instance
            
            await producer_service.start()
            
            mock_instance.start.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_send_email_dispatch_success(self):
        """Test successful email dispatch"""
        producer_service = KafkaProducerService()
        producer_service.producer = AsyncMock()
        
        email_data = {
            "recipientEmail": "test@test.com",
            "subject": "Test",
            "body": "Test body"
        }
        
        await producer_service.send_email_dispatch(email_data)
        
        producer_service.producer.send_and_wait.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_send_email_dispatch_failure(self):
        """Test email dispatch error handling"""
        producer_service = KafkaProducerService()
        producer_service.producer = AsyncMock()
        producer_service.producer.send_and_wait.side_effect = Exception("Kafka error")
        
        email_data = {"recipientEmail": "test@test.com", "subject": "Test", "body": "Body"}
        
        with pytest.raises(Exception, match="Kafka error"):
            await producer_service.send_email_dispatch(email_data)
    
    @pytest.mark.asyncio
    async def test_producer_stop(self):
        """Test producer shutdown"""
        producer_service = KafkaProducerService()
        producer_service.producer = AsyncMock()
        
        await producer_service.stop()
        
        producer_service.producer.stop.assert_called_once()
