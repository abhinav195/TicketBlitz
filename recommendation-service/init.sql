-- ============================================
-- Database Initialization Script
-- For Recommendation Service with pgvector
-- ============================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================
-- USER PREFERENCES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS user_preferences (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    preferred_categories TEXT[],
    preferred_price_range JSONB,
    preferred_locations TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id ON user_preferences(user_id);

-- ============================================
-- USER INTERACTIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS user_interactions (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    interaction_type VARCHAR(50) NOT NULL, -- 'view', 'click', 'bookmark', 'purchase'
    interaction_score FLOAT DEFAULT 1.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_interactions_user_id ON user_interactions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_interactions_event_id ON user_interactions(event_id);
CREATE INDEX IF NOT EXISTS idx_user_interactions_created_at ON user_interactions(created_at);

-- ============================================
-- EVENT EMBEDDINGS TABLE (for Vector Search)
-- ============================================
CREATE TABLE IF NOT EXISTS event_embeddings (
    id SERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL UNIQUE,
    event_title VARCHAR(500),
    event_description TEXT,
    category VARCHAR(100),
    tags TEXT[],
    embedding vector(384), -- Dimension depends on your embedding model
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_event_embeddings_event_id ON event_embeddings(event_id);
CREATE INDEX IF NOT EXISTS idx_event_embeddings_category ON event_embeddings(category);

-- Vector similarity search index (IVFFlat for better performance with large datasets)
-- Adjust lists parameter based on your dataset size
CREATE INDEX IF NOT EXISTS event_embeddings_vector_idx 
ON event_embeddings 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- ============================================
-- USER EMBEDDINGS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS user_embeddings (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    embedding vector(384), -- Should match event_embeddings dimension
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_embeddings_user_id ON user_embeddings(user_id);

-- Vector similarity search index
CREATE INDEX IF NOT EXISTS user_embeddings_vector_idx 
ON user_embeddings 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- ============================================
-- RECOMMENDATION CACHE TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS recommendation_cache (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recommended_event_ids BIGINT[],
    recommendation_scores FLOAT[],
    algorithm_used VARCHAR(50), -- 'collaborative', 'content_based', 'hybrid'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP + INTERVAL '1 hour'
);

CREATE INDEX IF NOT EXISTS idx_recommendation_cache_user_id ON recommendation_cache(user_id);
CREATE INDEX IF NOT EXISTS idx_recommendation_cache_expires_at ON recommendation_cache(expires_at);

-- ============================================
-- EVENT SIMILARITY TABLE (Precomputed)
-- ============================================
CREATE TABLE IF NOT EXISTS event_similarity (
    id SERIAL PRIMARY KEY,
    event_id_1 BIGINT NOT NULL,
    event_id_2 BIGINT NOT NULL,
    similarity_score FLOAT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(event_id_1, event_id_2)
);

CREATE INDEX IF NOT EXISTS idx_event_similarity_event_id_1 ON event_similarity(event_id_1);
CREATE INDEX IF NOT EXISTS idx_event_similarity_score ON event_similarity(similarity_score DESC);

-- ============================================
-- TRIGGERS for updated_at
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_user_preferences_updated_at
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_event_embeddings_updated_at
    BEFORE UPDATE ON event_embeddings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_embeddings_updated_at
    BEFORE UPDATE ON user_embeddings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- SEED DATA (Optional - for testing)
-- ============================================
-- Uncomment if you want some initial test data

-- INSERT INTO user_preferences (user_id, preferred_categories, preferred_price_range, preferred_locations)
-- VALUES 
--     (1, ARRAY['Music', 'Sports'], '{"min": 0, "max": 100}'::jsonb, ARRAY['New York', 'Los Angeles']),
--     (2, ARRAY['Theater', 'Comedy'], '{"min": 50, "max": 200}'::jsonb, ARRAY['Chicago', 'Boston']);

-- ============================================
-- HELPFUL VIEWS
-- ============================================

-- View for recent user interactions
CREATE OR REPLACE VIEW recent_user_interactions AS
SELECT 
    user_id,
    event_id,
    interaction_type,
    interaction_score,
    created_at
FROM user_interactions
WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
ORDER BY created_at DESC;

-- View for active recommendation cache
CREATE OR REPLACE VIEW active_recommendations AS
SELECT 
    user_id,
    recommended_event_ids,
    recommendation_scores,
    algorithm_used,
    created_at,
    expires_at
FROM recommendation_cache
WHERE expires_at > CURRENT_TIMESTAMP
ORDER BY created_at DESC;

-- ============================================
-- GRANTS (if needed for specific users)
-- ============================================
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

-- ============================================
-- Completion message
-- ============================================
DO $$
BEGIN
    RAISE NOTICE 'Recommendation database initialized successfully!';
    RAISE NOTICE 'pgvector extension enabled';
    RAISE NOTICE 'Created tables: user_preferences, user_interactions, event_embeddings, user_embeddings, recommendation_cache, event_similarity';
END $$;
