ALTER TABLE reservations ADD COLUMN IF NOT EXISTS payment_intent_id VARCHAR(100);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS auth_code VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_reservations_payment_intent ON reservations(payment_intent_id);
