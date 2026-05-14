CREATE INDEX `idx_wallet_transactions_type_status_created_at`
  ON `wallet_transactions`(`type`, `status`, `created_at`);

CREATE INDEX `idx_orders_status_created_at`
  ON `orders`(`status`, `created_at`);

CREATE INDEX `idx_orders_status_payment_time`
  ON `orders`(`status`, `payment_time`);

CREATE INDEX `idx_orders_status_shipped_at`
  ON `orders`(`status`, `shipped_at`);

CREATE INDEX `idx_refund_requests_status_updated_at`
  ON `refund_requests`(`status`, `updated_at`);
