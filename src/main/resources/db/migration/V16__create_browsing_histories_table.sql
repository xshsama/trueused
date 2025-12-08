CREATE TABLE IF NOT EXISTS browsing_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    viewed_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_browsing_histories_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_browsing_histories_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_browsing_history_user ON browsing_histories(user_id);
CREATE INDEX idx_browsing_history_viewed_at ON browsing_histories(viewed_at);
