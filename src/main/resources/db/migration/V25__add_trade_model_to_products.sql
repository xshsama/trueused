ALTER TABLE products ADD COLUMN trade_model VARCHAR(20);

UPDATE products SET trade_model = 'FREE_TRADING' WHERE trade_model IS NULL;
