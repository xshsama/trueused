CREATE TABLE wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    frozen_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
    pay_password VARCHAR(255) COMMENT '支付密码(加密)',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallets_user_id FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT='用户钱包表';

CREATE TABLE wallet_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL COMMENT '变动金额',
    type VARCHAR(20) NOT NULL COMMENT '交易类型: TOP_UP, PAYMENT, INCOME, REFUND, WITHDRAWAL',
    order_id BIGINT COMMENT '关联订单ID',
    status VARCHAR(20) NOT NULL COMMENT '状态: PENDING, SUCCESS, FAILED',
    remark VARCHAR(255) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_transactions_wallet_id FOREIGN KEY (wallet_id) REFERENCES wallets(id)
) COMMENT='钱包资金流水表';

-- 初始化现有用户的钱包 (可选，如果需要给现有用户创建钱包)
INSERT INTO wallets (user_id, balance, frozen_amount, version)
SELECT id, 0.00, 0.00, 0
FROM users
WHERE id NOT IN (SELECT user_id FROM wallets);
