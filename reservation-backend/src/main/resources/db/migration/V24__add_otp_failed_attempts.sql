ALTER TABLE verification_codes ADD COLUMN IF NOT EXISTS failed_attempts INT NOT NULL DEFAULT 0;
