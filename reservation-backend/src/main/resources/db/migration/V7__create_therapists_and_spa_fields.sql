CREATE TABLE IF NOT EXISTS therapists (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    specialty VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

INSERT INTO therapists (name, specialty, phone, active) VALUES
('Elena Vázquez', 'Masajes Relajantes & Aromaterapia', '+52 998 123 4567', true),
('Carlos Méndez', 'Tejido Profundo & Descontracturante', '+52 998 234 5678', true),
('Sofía Rivas', 'Rituales de Bienestar & Baños de Sales', '+52 998 345 6789', true);

ALTER TABLE room_orders
ADD COLUMN IF NOT EXISTS therapist_id BIGINT,
ADD COLUMN IF NOT EXISTS therapist_name VARCHAR(150),
ADD COLUMN IF NOT EXISTS service_location VARCHAR(50) DEFAULT 'IN_SUITE',
ADD COLUMN IF NOT EXISTS scheduled_time VARCHAR(50);
