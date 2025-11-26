-- 添加物流信息字段到订单表
ALTER TABLE orders ADD COLUMN tracking_number VARCHAR(50);
ALTER TABLE orders ADD COLUMN express_company VARCHAR(50);
ALTER TABLE orders ADD COLUMN express_code VARCHAR(20);
ALTER TABLE orders ADD COLUMN shipped_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN estimated_delivery_time TIMESTAMP;
ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP;

-- 为快递单号创建索引
CREATE INDEX idx_orders_tracking_number ON orders(tracking_number);
