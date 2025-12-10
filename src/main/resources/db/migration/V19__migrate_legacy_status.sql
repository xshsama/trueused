-- 1. Modify columns to VARCHAR to allow new status values (and remove ENUM restriction if present)
ALTER TABLE products MODIFY COLUMN status VARCHAR(50);
ALTER TABLE orders MODIFY COLUMN status VARCHAR(50);

-- 2. Migrate legacy ProductStatus values
UPDATE products SET status = 'ON_SALE' WHERE status = 'AVAILABLE';
UPDATE products SET status = 'SOLD_OUT' WHERE status = 'SOLD';
UPDATE products SET status = 'LOCKED' WHERE status = 'PENDING';
UPDATE products SET status = 'CANCELLED' WHERE status = 'HIDDEN';

-- 3. Migrate legacy OrderStatus values
UPDATE orders SET status = 'PENDING_PAYMENT' WHERE status = 'PENDING';
UPDATE orders SET status = 'REFUNDING' WHERE status = 'REFUND_PENDING';