-- Wallet transaction idempotency key (compatible with MySQL versions without IF NOT EXISTS on DDL)
SET @schema_name = DATABASE();

SET @biz_no_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'wallet_transactions'
    AND COLUMN_NAME = 'biz_no'
);

SET @sql = IF(
  @biz_no_column_exists = 0,
  'ALTER TABLE `wallet_transactions` ADD COLUMN `biz_no` VARCHAR(64) NULL AFTER `order_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @unique_index_exists = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'wallet_transactions'
    AND INDEX_NAME = 'uk_wallet_transactions_biz_no'
);

SET @sql = IF(
  @unique_index_exists = 0,
  'CREATE UNIQUE INDEX `uk_wallet_transactions_biz_no` ON `wallet_transactions` (`biz_no`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @wallet_created_idx_exists = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'wallet_transactions'
    AND INDEX_NAME = 'idx_wallet_transactions_wallet_created_at'
);

SET @sql = IF(
  @wallet_created_idx_exists = 0,
  'CREATE INDEX `idx_wallet_transactions_wallet_created_at` ON `wallet_transactions` (`wallet_id`, `created_at`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
