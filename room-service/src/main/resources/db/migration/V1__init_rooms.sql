CREATE TABLE IF NOT EXISTS rooms (
    id VARCHAR(36) PRIMARY KEY,
    number VARCHAR(20) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    capacity INT NOT NULL,
    price_per_night NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS room_bookings (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed rooms
INSERT INTO rooms (id, number, type, capacity, price_per_night) VALUES
('r-101', '101', 'SIMPLE', 1, 80.00),
('r-102', '102', 'DOUBLE', 2, 120.00),
('r-201', '201', 'DOUBLE', 2, 135.00),
('r-202', '202', 'DOUBLE', 2, 140.00),
('r-301', '301', 'SUITE', 4, 250.00),
('r-302', '302', 'SUITE', 4, 320.00)
ON CONFLICT (id) DO NOTHING;
