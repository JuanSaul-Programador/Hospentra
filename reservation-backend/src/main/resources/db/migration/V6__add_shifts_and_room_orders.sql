ALTER TABLE users 
ADD COLUMN IF NOT EXISTS shift VARCHAR(50) DEFAULT 'MORNING',
ADD COLUMN IF NOT EXISTS work_status VARCHAR(50) DEFAULT 'ON_DUTY',
ADD COLUMN IF NOT EXISTS break_started_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS extra_services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    image_url VARCHAR(255),
    available BOOLEAN DEFAULT true NOT NULL
);

INSERT INTO extra_services (name, category, description, price, image_url, available) VALUES
('Solomillo de Autor al Malbec', 'GASTRONOMY', 'Medallón de lomo fino en reducción de Malbec con puré rústico trufado.', 48.00, 'restaurant', true),
('Tabla Ibérica & Quesos Artesanales', 'GASTRONOMY', 'Selección de jamón ibérico de bellota, queso manchego curado y frutos secos.', 36.00, 'lunch_dining', true),
('Desayuno Imperial a la Suite', 'GASTRONOMY', 'Huevos benedictinos sobre brioche, tostadas francesas, zumo natural y café de grano.', 28.00, 'breakfast_dining', true),
('Moët & Chandon Brut Impérial', 'BAR_WINE', 'Champagne francés de prestigio servido en cubitera con copas de cristal templado.', 110.00, 'wine_bar', true),
('Vino Tinto Gran Reserva Privada', 'BAR_WINE', 'Crianza de barrica de roble con cuerpo aterciopelado y notas a frutos negros.', 65.00, 'liquor', true),
('Cóctel Signature Hospentra', 'BAR_WINE', 'Creación exclusiva de mixología boutique a base de ginebra botánica y cítricos.', 18.00, 'local_bar', true),
('Masaje Relajante en Suite (60 min)', 'WELLNESS_SPA', 'Terapia manual de relajación profunda con aceites esenciales orgánicos en su suite.', 85.00, 'spa', true),
('Ritual de Aromaterapia & Baño de Sales', 'WELLNESS_SPA', 'Acondicionamiento de baño de inmersión con sales del Himalaya y esencias botánicas.', 50.00, 'self_improvement', true);

CREATE TABLE IF NOT EXISTS room_orders (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    room_number VARCHAR(20) NOT NULL,
    guest_name VARCHAR(150) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    ordered_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    CONSTRAINT fk_room_orders_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS room_order_items (
    id BIGSERIAL PRIMARY KEY,
    room_order_id BIGINT NOT NULL,
    extra_service_id BIGINT,
    service_name VARCHAR(150) NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    quantity INT NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (room_order_id) REFERENCES room_orders(id) ON DELETE CASCADE
);
