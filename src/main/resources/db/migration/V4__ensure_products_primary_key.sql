-- Ensure products table has a numeric primary key `id`
-- Compatible with MySQL 8+/MariaDB

-- 1) Detect current state
SET @has_pk = (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'products'
    AND constraint_type = 'PRIMARY KEY'
);

SET @has_id = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'products'
    AND column_name = 'id'
);

-- 2) Add `id` column if missing
SET @sql_add_id = IF(@has_id = 0,
  'ALTER TABLE products ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST',
  'SELECT 1');
PREPARE stmt FROM @sql_add_id; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Ensure `id` is NOT NULL and AUTO_INCREMENT (in case it existed but was nullable/non-auto-inc)
SET @is_auto_inc = (
  SELECT EXTRA LIKE '%auto_increment%'
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'products'
    AND column_name = 'id'
  LIMIT 1
);

SET @sql_fix_id = IF(@is_auto_inc = 0,
  'ALTER TABLE products MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE stmt FROM @sql_fix_id; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) Add primary key if missing
SET @sql_add_pk = IF(@has_pk = 0,
  'ALTER TABLE products ADD PRIMARY KEY (id)',
  'SELECT 1');
PREPARE stmt FROM @sql_add_pk; EXECUTE stmt; DEALLOCATE PREPARE stmt;
