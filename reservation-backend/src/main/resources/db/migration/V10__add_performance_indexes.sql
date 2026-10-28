CREATE INDEX IF NOT EXISTS idx_rooms_room_type_id ON rooms(room_type_id);

CREATE INDEX IF NOT EXISTS idx_reservations_guest_id ON reservations(guest_id);
CREATE INDEX IF NOT EXISTS idx_reservations_room_id ON reservations(room_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_reservations_dates ON reservations(check_in_date, check_out_date);
CREATE INDEX IF NOT EXISTS idx_reservations_overlap ON reservations(room_id, status, check_in_date, check_out_date);

CREATE INDEX IF NOT EXISTS idx_room_orders_reservation_id ON room_orders(reservation_id);
CREATE INDEX IF NOT EXISTS idx_room_orders_status_ordered ON room_orders(status, ordered_at);
CREATE INDEX IF NOT EXISTS idx_room_orders_therapist_slot ON room_orders(therapist_id, scheduled_time, status);

CREATE INDEX IF NOT EXISTS idx_room_order_items_order_id ON room_order_items(room_order_id);

CREATE INDEX IF NOT EXISTS idx_users_guest_id ON users(guest_id);
