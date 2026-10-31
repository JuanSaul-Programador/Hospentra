ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_requested_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS scheduled_deletion_date TIMESTAMP WITHOUT TIME ZONE;
CREATE INDEX IF NOT EXISTS idx_users_scheduled_deletion ON users(scheduled_deletion_date) WHERE scheduled_deletion_date IS NOT NULL;
