CREATE TABLE inspections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    order_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    result_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE inspection_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sequence_order INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE inspection_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    image_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (inspection_id) REFERENCES inspections(id),
    FOREIGN KEY (item_id) REFERENCES inspection_items(id)
);

-- Seed data for inspection_items
INSERT INTO inspection_items (name, description, sequence_order) VALUES
('Exterior Condition', 'Check for scratches, dents, or other physical damage on the device body.', 1),
('Screen & Display', 'Verify screen condition, touch responsiveness, and display quality (no dead pixels).', 2),
('Hardware Functionality', 'Test buttons, ports, cameras, speakers, and microphone.', 3),
('Battery Health', 'Check battery capacity and charging functionality.', 4),
('Software & Accounts', 'Ensure device is reset, no iCloud/Google locks, and OS functions correctly.', 5);
