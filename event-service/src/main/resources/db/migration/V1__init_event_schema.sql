-- 1. Create Categories Table
CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(255) NOT NULL UNIQUE,
                            description VARCHAR(255)
);

-- 2. Create Events Table
CREATE TABLE events (
                        id BIGSERIAL PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description VARCHAR(2000),
                        date TIMESTAMP NOT NULL,
                        location VARCHAR(255) NOT NULL,
                        price DECIMAL(19, 2) NOT NULL,
                        total_tickets INTEGER NOT NULL,
                        available_tickets INTEGER NOT NULL,
                        category_id BIGINT NOT NULL,
                        version BIGINT,

    -- Foreign Key Constraint
                        CONSTRAINT fk_event_category
                            FOREIGN KEY (category_id)
                                REFERENCES categories (id)
);

-- 3. Create Event Images Table (ElementCollection)
CREATE TABLE event_images (
                              event_id BIGINT NOT NULL,
                              image_url VARCHAR(255),

                              CONSTRAINT fk_images_event
                                  FOREIGN KEY (event_id)
                                      REFERENCES events (id)
                                      ON DELETE CASCADE
);

-- 4. Create Indexes (from JPA @Index)
CREATE INDEX idx_event_date ON events(date);
CREATE INDEX idx_event_category ON events(category_id);
