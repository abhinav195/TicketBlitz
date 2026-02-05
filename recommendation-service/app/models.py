from sqlalchemy import Column, BigInteger, String, Text, DateTime, Index, Integer
from sqlalchemy.dialects.postgresql import ARRAY
from pgvector.sqlalchemy import Vector
from datetime import datetime
from app.db import Base
from app.settings import get_settings

settings = get_settings()


class EventVector(Base):
    """Stores event embeddings and metadata"""
    __tablename__ = "event_vectors"
    
    event_id = Column(BigInteger, primary_key=True, index=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=False)
    category = Column(String(100), nullable=False, index=True)
    location = Column(String(255), nullable=False)
    price = Column(String(50), nullable=False)
    date = Column(DateTime, nullable=False)
    image_urls = Column(ARRAY(Text), default=[])
    
    # Vector embedding (768 dimensions for text-embedding-004)
    embedding = Column(Vector(settings.EMBEDDING_DIMENSION), nullable=False)
    
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    
    __table_args__ = (
        Index(
            'idx_event_embedding_cosine',
            'embedding',
            postgresql_using='ivfflat',
            postgresql_with={'lists': 100},
            postgresql_ops={'embedding': 'vector_cosine_ops'}
        ),
    )


class UserHistory(Base):
    """Stores user booking history with event embeddings"""
    __tablename__ = "user_history"
    
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, nullable=False, index=True)
    event_id = Column(BigInteger, nullable=False)
    username = Column(String(100), nullable=False)
    email = Column(String(255), nullable=False)
    
    # Copy of the event embedding (denormalized for fast similarity search)
    event_embedding = Column(Vector(settings.EMBEDDING_DIMENSION), nullable=False)
    
    booked_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    
    __table_args__ = (
        Index('idx_user_id_booked_at', 'user_id', 'booked_at'),
        Index(
            'idx_user_history_embedding',
            'event_embedding',
            postgresql_using='ivfflat',
            postgresql_with={'lists': 50},
            postgresql_ops={'event_embedding': 'vector_cosine_ops'}
        ),
    )
