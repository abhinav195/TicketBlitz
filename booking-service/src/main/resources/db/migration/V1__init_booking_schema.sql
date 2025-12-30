CREATE TABLE booking (
                         id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         event_id BIGINT NOT NULL,
                         ticket_count INT NOT NULL,
                         total_price DECIMAL(10, 2) NOT NULL,
                         booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- New Column
                         status VARCHAR(20) NOT NULL
);

CREATE INDEX idx_booking_user ON booking(user_id);

-- Optional: Trigger to auto-update 'updated_at' on row changes (Postgres specific)
-- If you use @UpdateTimestamp in Java, this trigger is strictly optional but good for DB purity.
CREATE OR REPLACE FUNCTION update_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_booking_timestamp
    BEFORE UPDATE ON booking
    FOR EACH ROW
EXECUTE PROCEDURE update_timestamp();