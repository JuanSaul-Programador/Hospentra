INSERT INTO room_types (name, description, capacity, price_per_night) VALUES
('Suite Royal Vista al Mar', 'Espaciosa suite con ventanales de piso a techo frente al océano, acabados en roble natural y terraza privada con jacuzzi.', 3, 380.00),
('Habitación Doble Deluxe Jardín', 'Diseño contemporáneo con vistas a los jardines zen del hotel, aislamiento acústico total y amenities botánicos orgánicos.', 2, 250.00),
('Penthouse Panorámico Hospentra', 'El máximo exponente del lujo boutique con 360 grados de horizonte, sala de estar independiente y acceso a lounge VIP.', 4, 620.00),
('Villa Serena con Alberca Privada', 'Refugio idílico rodeado de palmeras y vegetación tropical, máxima privacidad con alberca climatizada de agua salada.', 4, 540.00),
('Studio Loft Arquitectónico', 'Estética minimalista con doble altura, luz natural envolvente y mobiliario de autor en nogal y concreto pulido.', 2, 290.00),
('Grand Master Suite Presidencial', 'Exclusividad absoluta con dos niveles, comedor privado para seis comensales, solárium en terraza y cava de vinos.', 6, 850.00)
ON CONFLICT (name) DO NOTHING;

INSERT INTO rooms (room_number, status, room_type_id) VALUES
('101', 'AVAILABLE', 1),
('202', 'AVAILABLE', 2),
('301', 'AVAILABLE', 3),
('401', 'AVAILABLE', 4),
('502', 'AVAILABLE', 5),
('601', 'AVAILABLE', 6)
ON CONFLICT (room_number) DO NOTHING;
