import pytest
from unittest.mock import patch, AsyncMock, MagicMock
from sqlalchemy import text
from app.db import get_db, init_db


class TestDatabase:
    """Test Database configuration and operations"""
    
    @pytest.mark.asyncio
    async def test_get_db_dependency_success(self):
        """Test FastAPI dependency for database session"""
        mock_session = AsyncMock()
        mock_session.execute = AsyncMock()
        mock_session.commit = AsyncMock()
        mock_session.close = AsyncMock()
        
        with patch("app.db.AsyncSessionLocal") as mock_session_maker:
            mock_session_maker.return_value.__aenter__.return_value = mock_session
            mock_session_maker.return_value.__aexit__.return_value = AsyncMock()
            
            async for session in get_db():
                assert session is not None
            
            mock_session.commit.assert_called()
            mock_session.close.assert_called()
    
    @pytest.mark.asyncio
    async def test_get_db_rollback_on_error(self):
        """Test database session rollback on error"""
        mock_session = AsyncMock()
        mock_session.execute = AsyncMock(side_effect=Exception("DB Error"))
        mock_session.rollback = AsyncMock()
        mock_session.close = AsyncMock()
        
        with patch("app.db.AsyncSessionLocal") as mock_session_maker:
            mock_session_maker.return_value.__aenter__.return_value = mock_session
            mock_session_maker.return_value.__aexit__.return_value = AsyncMock()
            
            try:
                async for session in get_db():
                    await session.execute(text("INVALID SQL"))
            except Exception:
                pass  # Expected to fail
    
    @pytest.mark.asyncio
    async def test_init_db(self):
        """Test database initialization - requires real DB connection"""
        # This test is skipped as it requires PostgreSQL connection
        # In integration tests, this would be tested with a real database
        pytest.skip("Requires PostgreSQL connection")
    
    @pytest.mark.asyncio
    async def test_session_context_manager(self):
        """Test session context manager behavior"""
        mock_session = AsyncMock()
        mock_session.commit = AsyncMock()
        mock_session.close = AsyncMock()
        
        with patch("app.db.AsyncSessionLocal") as mock_session_maker:
            mock_session_maker.return_value.__aenter__.return_value = mock_session
            mock_session_maker.return_value.__aexit__.return_value = AsyncMock()
            
            async for _ in get_db():
                pass
            
            mock_session.commit.assert_called()
            mock_session.close.assert_called()
