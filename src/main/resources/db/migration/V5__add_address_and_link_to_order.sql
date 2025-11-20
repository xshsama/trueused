-- V5: Add Address functionality and link it to Orders (Final, basic SQL)

CREATE TABLE addresses (
    id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    province VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    district VARCHAR(255) NOT NULL,
    detailed_address VARCHAR(255) NOT NULL,
    is_default BOOLEAN
);

ALTER TABLE addresses ADD PRIMARY KEY (id);

ALTER TABLE addresses
    ADD CONSTRAINT fk_addresses_users
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE orders
    ADD COLUMN address_id BIGINT;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_addresses
    FOREIGN KEY (address_id) REFERENCES addresses(id);