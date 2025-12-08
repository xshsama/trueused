CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_comments_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id)
);

CREATE INDEX idx_comments_product ON comments (product_id);
CREATE INDEX idx_comments_user ON comments (user_id);
