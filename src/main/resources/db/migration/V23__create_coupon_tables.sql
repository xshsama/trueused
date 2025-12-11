CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    discount_amount DECIMAL(10, 2) NOT NULL,
    min_spend DECIMAL(10, 2) DEFAULT 0,
    valid_days INT DEFAULT 30,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    claimed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    used_at DATETIME,
    valid_until DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);

-- Insert some sample coupons
INSERT INTO coupons (code, title, description, discount_amount, min_spend, valid_days) VALUES 
('WELCOME2024', '新人见面礼', '全场通用，无门槛立减', 50.00, 0.00, 30),
('DIGITAL100', '数码专享券', '仅限手机/电脑/平板品类使用', 100.00, 2000.00, 15),
('SHIPFREE', '运费抵扣券', '抵扣运费，最高20元', 20.00, 0.00, 60);
