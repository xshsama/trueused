DROP PROCEDURE IF EXISTS p_add_cols_v29;

DELIMITER $$
CREATE PROCEDURE p_add_cols_v29()
BEGIN
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'views_count') THEN
        ALTER TABLE products ADD COLUMN views_count BIGINT NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'favorites_count') THEN
        ALTER TABLE products ADD COLUMN favorites_count BIGINT NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'lat') THEN
        ALTER TABLE products ADD COLUMN lat DOUBLE;
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'lng') THEN
        ALTER TABLE products ADD COLUMN lng DOUBLE;
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'is_deleted') THEN
        ALTER TABLE products ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'location_text') THEN
        ALTER TABLE products ADD COLUMN location_text VARCHAR(100);
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'currency') THEN
        ALTER TABLE products ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'CNY';
    END IF;
END $$
DELIMITER ;

CALL p_add_cols_v29();
DROP PROCEDURE p_add_cols_v29;

CREATE TABLE IF NOT EXISTS product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    product_id BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id)
);

DROP PROCEDURE IF EXISTS p_add_idx_v29;
DELIMITER $$
CREATE PROCEDURE p_add_idx_v29()
BEGIN
    IF NOT EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'product_images' AND index_name = 'idx_images_product') THEN
        CREATE INDEX idx_images_product ON product_images (product_id);
    END IF;
END $$
DELIMITER ;
CALL p_add_idx_v29();
DROP PROCEDURE p_add_idx_v29;
