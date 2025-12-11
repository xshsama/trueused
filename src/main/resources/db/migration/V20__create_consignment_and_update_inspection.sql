CREATE TABLE consignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    expected_price DECIMAL(10, 2),
    shipping_method VARCHAR(255),
    tracking_no_inbound VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    product_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_consignment_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_consignment_product FOREIGN KEY (product_id) REFERENCES products(id)
);

ALTER TABLE inspections ADD COLUMN consignment_id BIGINT;
ALTER TABLE inspections ADD CONSTRAINT fk_inspection_consignment FOREIGN KEY (consignment_id) REFERENCES consignments(id);
ALTER TABLE inspections MODIFY COLUMN order_id BIGINT NULL;
