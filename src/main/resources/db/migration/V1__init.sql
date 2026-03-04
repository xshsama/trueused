/*
 * V1__init.sql
 * Consolidated Schema Initialization
 */

-- Disable foreign key checks for bulk creation
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------
-- Table `users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL,
  `email` VARCHAR(120) NOT NULL,
  `password` VARCHAR(200) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `nickname` VARCHAR(50),
  `avatar_url` VARCHAR(255),
  `bio` VARCHAR(300),
  `phone` VARCHAR(20),
  `email_verified` BOOLEAN DEFAULT FALSE,
  `phone_verified` BOOLEAN DEFAULT FALSE,
  `last_login_at` DATETIME(6),
  `ban_reason` VARCHAR(200),
  `ban_until` DATETIME(6),
  `cover_image` VARCHAR(255),
  `location` VARCHAR(255),
  `auto_reply_enabled` BOOLEAN DEFAULT FALSE,
  `auto_reply_text` VARCHAR(500),
  `vacation_mode` BOOLEAN DEFAULT FALSE,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `uk_users_username` UNIQUE (`username`),
  CONSTRAINT `uk_users_email` UNIQUE (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_users_username` ON `users`(`username`);
CREATE INDEX `idx_users_email` ON `users`(`email`);
CREATE INDEX `idx_users_nickname` ON `users`(`nickname`);

-- -----------------------------------------------------
-- Table `roles`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `roles` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(200),
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `uk_roles_name` UNIQUE (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `user_roles`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_roles` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  CONSTRAINT `uk_user_roles` UNIQUE (`user_id`, `role_id`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `addresses`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `addresses` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `recipient_name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(50),
  `city` VARCHAR(50),
  `district` VARCHAR(50),
  `detailed_address` VARCHAR(255),
  `area_code` VARCHAR(20),
  `is_default` BOOLEAN DEFAULT FALSE,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_addresses_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `categories`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `categories` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `parent_id` BIGINT,
  `name` VARCHAR(50) NOT NULL,
  `slug` VARCHAR(50),
  `path` VARCHAR(200),
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `uk_categories_slug` UNIQUE (`slug`),
  CONSTRAINT `uk_categories_name_parent` UNIQUE (`name`, `parent_id`),
  CONSTRAINT `fk_categories_parent` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `products`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `products` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `seller_id` BIGINT NOT NULL,
  `category_id` BIGINT,
  `title` VARCHAR(120) NOT NULL,
  `description` TEXT NOT NULL,
  `price` DECIMAL(12,2) NOT NULL,
  `original_price` DECIMAL(12,2),
  `heat_score` DOUBLE DEFAULT 0.0,
  `currency` VARCHAR(3) NOT NULL DEFAULT 'CNY',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `item_condition` VARCHAR(20),
  `location_text` VARCHAR(100),
  `shipping_payer` VARCHAR(20),
  `trade_types` VARCHAR(50),
  `trade_model` VARCHAR(20) DEFAULT 'FREE_TRADING',
  `lat` DOUBLE,
  `lng` DOUBLE,
  `views_count` BIGINT NOT NULL DEFAULT 0,
  `favorites_count` BIGINT NOT NULL DEFAULT 0,
  `is_deleted` BOOLEAN DEFAULT FALSE,
  `last_polished_at` DATETIME(6),
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_products_seller` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_products_seller` ON `products`(`seller_id`);
CREATE INDEX `idx_products_category` ON `products`(`category_id`);
CREATE INDEX `idx_products_price` ON `products`(`price`);
CREATE INDEX `idx_products_created_at` ON `products`(`created_at`);
CREATE INDEX `idx_products_title` ON `products`(`title`);

-- -----------------------------------------------------
-- Table `product_images`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_images` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL,
  `image_key` VARCHAR(255) NOT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `is_cover` BOOLEAN DEFAULT FALSE,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_product_images_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `orders`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `buyer_id` BIGINT NOT NULL,
  `seller_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `address_id` BIGINT NOT NULL,
  `price` DECIMAL(19,2) NOT NULL,
  `discount_amount` DECIMAL(19,2) DEFAULT 0.00,
  `status` VARCHAR(255) NOT NULL,
  `payment_time` DATETIME(6),
  `transaction_id` VARCHAR(255),
  `tracking_number` VARCHAR(50),
  `express_company` VARCHAR(50),
  `express_code` VARCHAR(20),
  `shipped_at` DATETIME(6),
  `estimated_delivery_time` DATETIME(6),
  `delivered_at` DATETIME(6),
  `product_snapshot` TEXT,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_orders_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_orders_seller` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_orders_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_orders_address` FOREIGN KEY (`address_id`) REFERENCES `addresses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `consignments`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `consignments` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `seller_id` BIGINT NOT NULL,
  `product_id` BIGINT,
  `category_id` BIGINT,
  `title` VARCHAR(255),
  `description` TEXT,
  `expected_price` DECIMAL(10,2),
  `original_price` DECIMAL(10,2),
  `shipping_method` VARCHAR(255),
  `tracking_no_inbound` VARCHAR(255),
  `status` VARCHAR(255) NOT NULL DEFAULT 'CREATED',
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_consignments_seller` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_consignments_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_consignments_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `inspections`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspections` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT,
  `consignment_id` BIGINT,
  `status` VARCHAR(255) NOT NULL,
  `result_summary` VARCHAR(255),
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_inspections_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_inspections_consignment` FOREIGN KEY (`consignment_id`) REFERENCES `consignments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `inspection_items`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT,
  `sequence_order` INT NOT NULL,
  `template_type` VARCHAR(50),
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `inspection_results`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_results` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `inspection_id` BIGINT NOT NULL,
  `item_id` BIGINT NOT NULL,
  `status` VARCHAR(255) NOT NULL,
  `notes` TEXT,
  `image_url` VARCHAR(255),
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_inspection_results_inspection` FOREIGN KEY (`inspection_id`) REFERENCES `inspections` (`id`),
  CONSTRAINT `fk_inspection_results_item` FOREIGN KEY (`item_id`) REFERENCES `inspection_items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `reviews`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `reviews` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `buyer_id` BIGINT NOT NULL,
  `seller_id` BIGINT NOT NULL,
  `rating` INT NOT NULL,
  `content` TEXT,
  `is_anonymous` BOOLEAN DEFAULT FALSE,
  `seller_reply` TEXT,
  `seller_reply_at` DATETIME(6),
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `uk_reviews_order` UNIQUE (`order_id`),
  CONSTRAINT `fk_reviews_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_reviews_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_reviews_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_reviews_seller` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `review_images` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `review_id` BIGINT NOT NULL,
  `url` VARCHAR(512) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_review_images_review` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `coupons`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `coupons` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(255) NOT NULL,
  `type` VARCHAR(255) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `description` VARCHAR(255),
  `discount_amount` DECIMAL(19,2) NOT NULL,
  `min_spend` DECIMAL(19,2) DEFAULT 0.00,
  `valid_days` INT DEFAULT 30,
  `is_active` BOOLEAN DEFAULT TRUE,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `uk_coupons_code` UNIQUE (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_coupons` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `coupon_id` BIGINT NOT NULL,
  `is_used` BOOLEAN DEFAULT FALSE,
  `claimed_at` DATETIME(6),
  `used_at` DATETIME(6),
  `valid_until` DATETIME(6),
  CONSTRAINT `fk_user_coupons_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_user_coupons_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `chat_session`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id_a` BIGINT NOT NULL,
  `user_id_b` BIGINT NOT NULL,
  `last_message_content` VARCHAR(1000),
  `last_message_time` DATETIME(6),
  `unread_count_a` INT NOT NULL DEFAULT 0,
  `unread_count_b` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `uk_chat_session_users` UNIQUE (`user_id_a`, `user_id_b`),
  CONSTRAINT `fk_chat_session_user_a` FOREIGN KEY (`user_id_a`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_chat_session_user_b` FOREIGN KEY (`user_id_b`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `session_id` BIGINT NOT NULL,
  `sender_id` BIGINT NOT NULL,
  `receiver_id` BIGINT NOT NULL,
  `content` TEXT,
  `is_read` BOOLEAN DEFAULT FALSE,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_chat_messages_session` FOREIGN KEY (`session_id`) REFERENCES `chat_session` (`id`),
  CONSTRAINT `fk_chat_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_chat_messages_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `favorites`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `favorites` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `note` VARCHAR(200),
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `uk_favorites_user_product` UNIQUE (`user_id`, `product_id`),
  CONSTRAINT `fk_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_favorites_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `browsing_histories`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `browsing_histories` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `viewed_at` DATETIME(6) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_browsing_histories_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_browsing_histories_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `wallets`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `wallets` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `balance` DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  `frozen_amount` DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  `pay_password` VARCHAR(255),
  `version` BIGINT,
  `created_at` DATETIME(6),
  `updated_at` DATETIME(6),
  CONSTRAINT `uk_wallets_user` UNIQUE (`user_id`),
  CONSTRAINT `fk_wallets_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `wallet_transactions`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `wallet_transactions` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `wallet_id` BIGINT NOT NULL,
  `amount` DECIMAL(19,2) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `order_id` BIGINT,
  `status` VARCHAR(50) NOT NULL,
  `remark` VARCHAR(255),
  `created_at` DATETIME(6),
  CONSTRAINT `fk_wallet_transactions_wallet` FOREIGN KEY (`wallet_id`) REFERENCES `wallets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `refund_requests`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `refund_requests` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `reason` VARCHAR(500),
  `refund_type` VARCHAR(50) NOT NULL,
  `status` VARCHAR(50) NOT NULL,
  `refund_amount` DECIMAL(19,2) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_refund_requests_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `notifications`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `content` VARCHAR(1000) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `related_id` BIGINT,
  `is_read` BOOLEAN DEFAULT FALSE,
  `created_at` DATETIME(6),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `comments`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `comments` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT,
  `target_user_id` BIGINT,
  `parent_id` BIGINT,
  `content` TEXT,
  `is_deleted` BOOLEAN DEFAULT FALSE,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_comments_target_user` FOREIGN KEY (`target_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_comments_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_comments_parent` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;
