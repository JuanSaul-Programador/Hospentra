CREATE TABLE IF NOT EXISTS concierge_messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reservation_id BIGINT REFERENCES reservations(id) ON DELETE SET NULL,
    sender_role VARCHAR(40) NOT NULL,
    sender_name VARCHAR(150) NOT NULL,
    suite_number VARCHAR(50),
    message TEXT NOT NULL,
    read_status BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_concierge_sender_created ON concierge_messages(sender_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_concierge_recipient_created ON concierge_messages(recipient_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_concierge_reservation_created ON concierge_messages(reservation_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_concierge_read_status ON concierge_messages(read_status);
