CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    seller_reply TEXT,
    seller_reply_at DATETIME(6),
    CONSTRAINT fk_reviews_orders FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_reviews_products FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_reviews_buyer FOREIGN KEY (buyer_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT uk_reviews_order UNIQUE (order_id)
);
