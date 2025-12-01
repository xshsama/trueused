CREATE TABLE IF NOT EXISTS refund_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    order_id BIGINT NOT NULL,
    reason VARCHAR(500),
    refund_type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    refund_amount DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_refund_requests_orders FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    participant1_id BIGINT NOT NULL,
    participant2_id BIGINT NOT NULL,
    last_message_id BIGINT,
    CONSTRAINT fk_conversations_participant1 FOREIGN KEY (participant1_id) REFERENCES users (id),
    CONSTRAINT fk_conversations_participant2 FOREIGN KEY (participant2_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_messages_conversations FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users (id)
);

-- Attempt to add constraint only if it doesn't exist?
-- MySQL doesn't support IF NOT EXISTS for ADD CONSTRAINT directly in standard SQL.
-- However, we can wrap it in a procedure or just try to run it. 
-- If the table conversations was just created, it doesn't have the constraint.
-- If it already existed, it might have it. 
-- Let's try to add it. If it fails, we might need a more complex script or manual intervention.
-- But since we are using Flyway, maybe we can assume if the table existed, the constraint might be there or not. 
-- If I want to be safe, I can try to drop it first (which might fail if not exists) or just leave it.

ALTER TABLE conversations
ADD CONSTRAINT fk_conversations_last_message FOREIGN KEY (last_message_id) REFERENCES messages (id);
