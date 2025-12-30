-- 1. Add a generated column for full-text search.
-- We combine Title (Weight A), Description (Weight B), and Location (Weight C)
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS search_vector tsvector
        GENERATED ALWAYS AS (
            setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
            setweight(to_tsvector('english', coalesce(description, '')), 'B') ||
            setweight(to_tsvector('english', coalesce(location, ''))::tsvector, 'C')
            ) STORED;

-- 2. Create the GIN Index for high-speed queries
CREATE INDEX IF NOT EXISTS idx_events_search_vector ON events USING GIN(search_vector);

-- 3. Validation constraint: Ensure basic data integrity for search
ALTER TABLE events ADD CONSTRAINT check_positive_price CHECK (price >= 0);
