CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    cleanliness_rating INT CHECK (cleanliness_rating >= 1 AND cleanliness_rating <= 5),
    service_rating INT CHECK (service_rating >= 1 AND service_rating <= 5),
    comfort_rating INT CHECK (comfort_rating >= 1 AND comfort_rating <= 5),
    location_rating INT CHECK (location_rating >= 1 AND location_rating <= 5),
    title VARCHAR(150),
    comment TEXT NOT NULL,
    management_response TEXT,
    management_responded_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_reviews_room_id ON reviews(room_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_rating ON reviews(room_id, rating);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews(created_at DESC);

INSERT INTO guests (first_name, last_name, email, phone)
VALUES 
('Sofía', 'Valdez', 'sofia.valdez@luxurytravel.com', '+52 998 123 4567'),
('Marcus', 'Vance', 'marcus.vance@executive.com', '+1 415 555 0192'),
('Elena', 'Rostova', 'elena.rostova@design.com', '+34 912 345 678')
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (username, email, password, role, enabled, created_at, guest_id)
SELECT 'sofia_valdez', 'sofia.valdez@luxurytravel.com', '$2a$10$NN3HTj4juG4JhA3cDjGFSOFggJRm01yWyxvMOeW1sItbQBakdNjbC', 'ROLE_CUSTOMER', true, NOW(), id FROM guests WHERE email = 'sofia.valdez@luxurytravel.com'
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password, role, enabled, created_at, guest_id)
SELECT 'marcus_vance', 'marcus.vance@executive.com', '$2a$10$NN3HTj4juG4JhA3cDjGFSOFggJRm01yWyxvMOeW1sItbQBakdNjbC', 'ROLE_CUSTOMER', true, NOW(), id FROM guests WHERE email = 'marcus.vance@executive.com'
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password, role, enabled, created_at, guest_id)
SELECT 'elena_rostova', 'elena.rostova@design.com', '$2a$10$NN3HTj4juG4JhA3cDjGFSOFggJRm01yWyxvMOeW1sItbQBakdNjbC', 'ROLE_CUSTOMER', true, NOW(), id FROM guests WHERE email = 'elena.rostova@design.com'
ON CONFLICT (username) DO NOTHING;

INSERT INTO reservations (guest_id, room_id, check_in_date, check_out_date, status, total_price, created_at)
SELECT g.id, 1, '2026-09-10', '2026-09-14', 'COMPLETED', 1520.00, '2026-09-01 10:00:00'
FROM guests g WHERE g.email = 'sofia.valdez@luxurytravel.com'
AND NOT EXISTS (SELECT 1 FROM reservations WHERE guest_id = g.id AND room_id = 1 AND check_in_date = '2026-09-10');

INSERT INTO reservations (guest_id, room_id, check_in_date, check_out_date, status, total_price, created_at)
SELECT g.id, 1, '2026-09-18', '2026-09-22', 'COMPLETED', 1520.00, '2026-09-05 11:30:00'
FROM guests g WHERE g.email = 'marcus.vance@executive.com'
AND NOT EXISTS (SELECT 1 FROM reservations WHERE guest_id = g.id AND room_id = 1 AND check_in_date = '2026-09-18');

INSERT INTO reservations (guest_id, room_id, check_in_date, check_out_date, status, total_price, created_at)
SELECT g.id, 1, '2026-09-25', '2026-09-28', 'COMPLETED', 1140.00, '2026-09-12 14:15:00'
FROM guests g WHERE g.email = 'elena.rostova@design.com'
AND NOT EXISTS (SELECT 1 FROM reservations WHERE guest_id = g.id AND room_id = 1 AND check_in_date = '2026-09-25');

INSERT INTO reservations (guest_id, room_id, check_in_date, check_out_date, status, total_price, created_at)
SELECT g.id, 1, '2026-10-02', '2026-10-05', 'COMPLETED', 1140.00, '2026-09-20 09:00:00'
FROM guests g WHERE g.email = 'maria@email.com'
AND NOT EXISTS (SELECT 1 FROM reservations WHERE guest_id = g.id AND room_id = 1 AND check_in_date = '2026-10-02');

INSERT INTO reservations (guest_id, room_id, check_in_date, check_out_date, status, total_price, created_at)
SELECT g.id, 1, '2026-10-10', '2026-10-13', 'COMPLETED', 1140.00, '2026-09-28 16:45:00'
FROM guests g WHERE g.email = 'palo@gmail.com'
AND NOT EXISTS (SELECT 1 FROM reservations WHERE guest_id = g.id AND room_id = 1 AND check_in_date = '2026-10-10');

INSERT INTO reservations (guest_id, room_id, check_in_date, check_out_date, status, total_price, created_at)
SELECT g.id, 1, '2026-10-15', '2026-10-18', 'COMPLETED', 1140.00, '2026-10-01 12:00:00'
FROM guests g WHERE g.email = 'pepe@gmail.com'
AND NOT EXISTS (SELECT 1 FROM reservations WHERE guest_id = g.id AND room_id = 1 AND check_in_date = '2026-10-15');

INSERT INTO reviews (reservation_id, user_id, room_id, rating, cleanliness_rating, service_rating, comfort_rating, location_rating, title, comment, created_at)
SELECT r.id, u.id, 1, 5, 5, 5, 5, 5, 'Una experiencia mágica frente al mar', 'La vista al amanecer desde la terraza privada y la serenidad del jacuzzi superaron todas nuestras expectativas. El servicio de Concierge fue impecable en cada detalle.', '2026-09-15 11:00:00'
FROM reservations r
JOIN guests g ON r.guest_id = g.id
JOIN users u ON u.guest_id = g.id
WHERE g.email = 'sofia.valdez@luxurytravel.com' AND r.room_id = 1 AND r.check_in_date = '2026-09-10'
ON CONFLICT (reservation_id) DO NOTHING;

INSERT INTO reviews (reservation_id, user_id, room_id, rating, cleanliness_rating, service_rating, comfort_rating, location_rating, title, comment, created_at)
SELECT r.id, u.id, 1, 5, 5, 5, 5, 5, 'Privacidad y confort ejecutivo de primer nivel', 'El estándar de privacidad y silencio es ideal para viajes ejecutivos y descanso. La conexión Wi-Fi es de alta velocidad y el servicio a la habitación llegó en menos de 20 minutos.', '2026-09-23 15:30:00'
FROM reservations r
JOIN guests g ON r.guest_id = g.id
JOIN users u ON u.guest_id = g.id
WHERE g.email = 'marcus.vance@executive.com' AND r.room_id = 1 AND r.check_in_date = '2026-09-18'
ON CONFLICT (reservation_id) DO NOTHING;

INSERT INTO reviews (reservation_id, user_id, room_id, rating, cleanliness_rating, service_rating, comfort_rating, location_rating, title, comment, created_at)
SELECT r.id, u.id, 1, 4, 5, 4, 4, 5, 'Arquitectura espectacular y detalles de autor', 'Diseño y acabados de madera extraordinarios. La tina de hidromasaje con vista al jardín y océano es un auténtico lujo. Sugiero únicamente ampliar las alternativas saludables en el menú.', '2026-09-29 18:20:00'
FROM reservations r
JOIN guests g ON r.guest_id = g.id
JOIN users u ON u.guest_id = g.id
WHERE g.email = 'elena.rostova@design.com' AND r.room_id = 1 AND r.check_in_date = '2026-09-25'
ON CONFLICT (reservation_id) DO NOTHING;

INSERT INTO reviews (reservation_id, user_id, room_id, rating, cleanliness_rating, service_rating, comfort_rating, location_rating, title, comment, management_response, management_responded_at, created_at)
SELECT r.id, u.id, 1, 3, 4, 3, 4, 5, 'Hermosa suite pero hubo demora en el check-in', 'El hotel y las amenidades son de primer nivel, pero tuvimos una espera de 20 minutos en la recepción durante el registro de entrada.', 'Estimada María, le agradecemos sinceramente su observación. Hemos implementado un nuevo protocolo de Check-In exprés digital para asegurar que la bienvenida sea inmediata. Será un placer recibirle de nuevo.', '2026-10-06 14:00:00', '2026-10-06 10:15:00'
FROM reservations r
JOIN guests g ON r.guest_id = g.id
JOIN users u ON u.guest_id = g.id
WHERE g.email = 'maria@email.com' AND r.room_id = 1 AND r.check_in_date = '2026-10-02'
ON CONFLICT (reservation_id) DO NOTHING;

INSERT INTO reviews (reservation_id, user_id, room_id, rating, cleanliness_rating, service_rating, comfort_rating, location_rating, title, comment, created_at)
SELECT r.id, u.id, 1, 5, 5, 5, 5, 5, 'El mejor santuario para desconectarse', 'Excelente ambiente para descansar. El masaje terapéutico de spa en la suite y la cena gourmet privada fueron momentos inolvidables.', '2026-10-14 12:45:00'
FROM reservations r
JOIN guests g ON r.guest_id = g.id
JOIN users u ON u.guest_id = g.id
WHERE g.email = 'palo@gmail.com' AND r.room_id = 1 AND r.check_in_date = '2026-10-10'
ON CONFLICT (reservation_id) DO NOTHING;
