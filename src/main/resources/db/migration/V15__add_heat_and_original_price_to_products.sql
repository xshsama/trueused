ALTER TABLE products
ADD COLUMN original_price DECIMAL(12, 2),
ADD COLUMN heat_score DOUBLE DEFAULT 0.0;

CREATE INDEX idx_products_heat_score ON products(heat_score);
