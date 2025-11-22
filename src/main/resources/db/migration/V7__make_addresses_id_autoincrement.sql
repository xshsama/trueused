-- Temporarily drop the foreign key constraint
ALTER TABLE orders DROP CONSTRAINT fk_orders_addresses;

-- Modify the id column to be auto-incrementing
ALTER TABLE addresses MODIFY id BIGINT NOT NULL AUTO_INCREMENT;

-- Re-add the foreign key constraint
ALTER TABLE orders
ADD CONSTRAINT fk_orders_addresses
FOREIGN KEY (address_id) REFERENCES addresses(id);