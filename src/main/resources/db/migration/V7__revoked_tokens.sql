CREATE TABLE IF NOT EXISTS `revoked_tokens` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `token_hash` CHAR(64) NOT NULL,
  `token_type` VARCHAR(20) NOT NULL,
  `expires_at` DATETIME(6) NOT NULL,
  `user_id` BIGINT NULL,
  `reason` VARCHAR(100),
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `uk_revoked_tokens_hash` UNIQUE (`token_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_revoked_tokens_expires_at` ON `revoked_tokens`(`expires_at`);
CREATE INDEX `idx_revoked_tokens_user_id` ON `revoked_tokens`(`user_id`);
