CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE venues (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(500) NOT NULL,
    timezone    VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id      UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    title         VARCHAR(255) NOT NULL,
    start_at      TIMESTAMPTZ NOT NULL,
    end_at        TIMESTAMPTZ NOT NULL,
    status        VARCHAR(30) NOT NULL,
    currency      VARCHAR(10) NOT NULL,
    pricing_rules JSONB NOT NULL,
    version       INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_event_venue_title_start UNIQUE (venue_id, title, start_at),
    CONSTRAINT chk_event_time_order CHECK (start_at < end_at)
);

CREATE TABLE seats (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id    UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    section     VARCHAR(50) NOT NULL,
    row_label   VARCHAR(20) NOT NULL,
    number      INT NOT NULL,
    category    VARCHAR(20) NOT NULL,
    attributes  JSONB NOT NULL DEFAULT '[]',
    CONSTRAINT uq_seat_position UNIQUE (venue_id, section, row_label, number)
);

CREATE TABLE reservations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    customer_email  VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    hold_expires_at TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at    TIMESTAMPTZ,
    version         INT NOT NULL DEFAULT 0
);

CREATE TABLE reservation_seats (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id    UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    seat_id           UUID NOT NULL REFERENCES seats(id),
    event_id          UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    price_amount      NUMERIC(12,2) NOT NULL,
    price_currency    VARCHAR(10) NOT NULL,
    discount          VARCHAR(30) NOT NULL DEFAULT 'NONE',
    CONSTRAINT uq_event_seat UNIQUE (event_id, seat_id)
);

-- Indexes for hot paths
CREATE INDEX idx_events_start_at            ON events (start_at);
CREATE INDEX idx_events_venue_id            ON events (venue_id);
CREATE INDEX idx_events_title_lower         ON events (LOWER(title));
CREATE INDEX idx_reservations_event_id      ON reservations (event_id);
CREATE INDEX idx_reservations_email         ON reservations (LOWER(customer_email));
CREATE INDEX idx_reservations_status        ON reservations (status);
CREATE INDEX idx_seats_venue_id             ON seats (venue_id);
CREATE INDEX idx_reservation_seats_event    ON reservation_seats (event_id);
CREATE INDEX idx_reservation_seats_seat     ON reservation_seats (seat_id);