ALTER TABLE orders
ADD COLUMN payment_time DATETIME(6),
ADD COLUMN transaction_id VARCHAR(255);
