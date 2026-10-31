CREATE TABLE IF NOT EXISTS maintenance_blocks (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    block_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT NOT NULL,
    contractor_name VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(100),
    resolved_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_maint_blocks_room ON maintenance_blocks(room_id);
CREATE INDEX IF NOT EXISTS idx_maint_blocks_dates ON maintenance_blocks(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_maint_blocks_status ON maintenance_blocks(status);
