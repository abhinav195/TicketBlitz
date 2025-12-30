-- 1. Drop the generated column (it's broken/stubborn)
ALTER TABLE events DROP COLUMN IF EXISTS search_vector;

-- 2. Add it back as a regular column
ALTER TABLE events ADD COLUMN search_vector tsvector;

-- 3. Create a Function to calculate the vector
CREATE OR REPLACE FUNCTION events_search_vector_trigger() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
            setweight(to_tsvector('english', coalesce(NEW.title, '')), 'A') ||
            setweight(to_tsvector('english', coalesce(NEW.description, '')), 'B') ||
            setweight(to_tsvector('english', coalesce(NEW.location, '')), 'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- 4. Create the Trigger
CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
    ON events FOR EACH ROW EXECUTE PROCEDURE events_search_vector_trigger();

-- 5. Force update existing rows
UPDATE events SET title = title;
