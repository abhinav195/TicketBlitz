import pytest
from datetime import datetime
from app.models import EventVector, UserHistory


class TestEventVector:
    """Test EventVector model"""
    
    def test_event_vector_creation(self):
        """Test EventVector instance creation"""
        event = EventVector(
            event_id=1,
            title="Test Event",
            description="Test Description",
            category="Music",
            location="New York",
            price="50.00",
            date=datetime.now(),
            image_urls=["http://example.com/image.jpg"],
            embedding=[0.1] * 768
        )
        
        assert event.event_id == 1
        assert event.title == "Test Event"
        assert event.category == "Music"
        assert len(event.embedding) == 768
    
    def test_event_vector_default_values(self):
        """Test default values for EventVector"""
        event = EventVector(
            event_id=2,
            title="Event 2",
            description="Desc",
            category="Sports",
            location="LA",
            price="100",
            date=datetime.now(),
            embedding=[0.2] * 768
        )
        
        # SQLAlchemy doesn't set defaults until committed to DB
        # This test verifies the object can be created without image_urls
        assert event.title == "Event 2"
        assert event.event_id == 2


class TestUserHistory:
    """Test UserHistory model"""
    
    def test_user_history_creation(self):
        """Test UserHistory instance creation"""
        history = UserHistory(
            user_id=10,
            event_id=5,
            username="testuser",
            email="test@test.com",
            event_embedding=[0.3] * 768
        )
        
        assert history.user_id == 10
        assert history.event_id == 5
        assert history.username == "testuser"
        assert history.email == "test@test.com"
        assert len(history.event_embedding) == 768
    
    def test_user_history_with_timestamp(self):
        """Test UserHistory with explicit timestamp"""
        now = datetime.now()
        history = UserHistory(
            user_id=20,
            event_id=10,
            username="user2",
            email="user2@test.com",
            event_embedding=[0.4] * 768,
            booked_at=now
        )
        
        assert history.booked_at == now
        assert isinstance(history.booked_at, datetime)
