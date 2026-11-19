CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id VARCHAR(36) NOT NULL UNIQUE,
    reservation_id VARCHAR(36) NOT NULL,
    client_id VARCHAR(36) NOT NULL,
    amount_ht DECIMAL(10, 2) NOT NULL,
    tva DECIMAL(10, 2) NOT NULL,
    amount_ttc DECIMAL(10, 2) NOT NULL,
    issue_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reservation_id (reservation_id)
);
