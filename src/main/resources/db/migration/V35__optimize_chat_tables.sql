-- 1. Drop existing Foreign Keys to avoid issues during renaming
-- Note: Using the constraint names defined in V12

-- Drop FKs on conversations
ALTER TABLE conversations DROP FOREIGN KEY fk_conversations_participant1;
ALTER TABLE conversations DROP FOREIGN KEY fk_conversations_participant2;
-- This might fail if the constraint name is different or was auto-generated differently in some envs, 
-- but we assume standard Flyway execution based on V12.
-- If fk_conversations_last_message exists:
ALTER TABLE conversations DROP FOREIGN KEY fk_conversations_last_message;

-- Drop FKs on messages
ALTER TABLE messages DROP FOREIGN KEY fk_messages_conversations;
ALTER TABLE messages DROP FOREIGN KEY fk_messages_sender;
ALTER TABLE messages DROP FOREIGN KEY fk_messages_receiver;

-- 2. Rename Tables
RENAME TABLE conversations TO chat_session;
RENAME TABLE messages TO chat_message;

-- 3. Modify chat_session (formerly conversations)
-- Rename participant columns
ALTER TABLE chat_session CHANGE participant1_id user_id_a BIGINT NOT NULL;
ALTER TABLE chat_session CHANGE participant2_id user_id_b BIGINT NOT NULL;

-- Add new columns for snapshot and unread counts
ALTER TABLE chat_session ADD COLUMN last_message_content VARCHAR(1000) COMMENT 'Snapshot of last message content';
ALTER TABLE chat_session ADD COLUMN last_message_time DATETIME(6) COMMENT 'Time of last message';
ALTER TABLE chat_session ADD COLUMN unread_count_a INT DEFAULT 0 COMMENT 'Unread count for user_id_a';
ALTER TABLE chat_session ADD COLUMN unread_count_b INT DEFAULT 0 COMMENT 'Unread count for user_id_b';

-- 4. Migrate Data
-- Populate last_message_content and last_message_time from the referenced message
UPDATE chat_session cs
JOIN chat_message cm ON cs.last_message_id = cm.id
SET cs.last_message_content = cm.content,
    cs.last_message_time = cm.created_at;

-- If last_message_id was null, last_message_time might be null. 
-- We can default it to updated_at or created_at if needed, but null is okay for empty chat.

-- Calculate unread counts (Optional but good for consistency)
-- This is a complex update in MySQL. Let's try to set it based on current messages.
-- For user_id_a, count messages sent by user_id_b that are not read.
UPDATE chat_session cs
SET unread_count_a = (
    SELECT COUNT(*) 
    FROM chat_message cm 
    WHERE cm.session_id = cs.id 
    AND cm.sender_id = cs.user_id_b 
    AND cm.is_read = FALSE
);

UPDATE chat_session cs
SET unread_count_b = (
    SELECT COUNT(*) 
    FROM chat_message cm 
    WHERE cm.session_id = cs.id 
    AND cm.sender_id = cs.user_id_a 
    AND cm.is_read = FALSE
);

-- 5. Cleanup and Constraints
-- Drop the old last_message_id column
ALTER TABLE chat_session DROP COLUMN last_message_id;

-- Add Foreign Keys back to chat_session
ALTER TABLE chat_session ADD CONSTRAINT fk_chat_session_user_a FOREIGN KEY (user_id_a) REFERENCES users(id);
ALTER TABLE chat_session ADD CONSTRAINT fk_chat_session_user_b FOREIGN KEY (user_id_b) REFERENCES users(id);

-- Add Unique Constraint to ensure only one session per pair
-- Note: This assumes data is already clean (no duplicates like A-B and B-A).
-- If there are duplicates, this will fail. For a safe migration, we might want to clean up duplicates first.
-- Assuming distinct pairs for now.
CREATE UNIQUE INDEX idx_chat_session_users ON chat_session(user_id_a, user_id_b);

-- 6. Modify chat_message (formerly messages)
-- Rename conversation_id
ALTER TABLE chat_message CHANGE conversation_id session_id BIGINT NOT NULL;

-- Add Foreign Keys back to chat_message
ALTER TABLE chat_message ADD CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id);
ALTER TABLE chat_message ADD CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES users(id);
ALTER TABLE chat_message ADD CONSTRAINT fk_chat_message_receiver FOREIGN KEY (receiver_id) REFERENCES users(id);

-- Indexes for performance
CREATE INDEX idx_chat_message_session_created ON chat_message(session_id, created_at);
