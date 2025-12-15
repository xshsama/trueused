-- Add target_user_id column to comments table
ALTER TABLE comments ADD COLUMN target_user_id BIGINT;

-- Add foreign key constraint for target_user_id
ALTER TABLE comments ADD CONSTRAINT fk_comments_target_user FOREIGN KEY (target_user_id) REFERENCES users (id);

-- Modify product_id to be nullable
ALTER TABLE comments MODIFY COLUMN product_id BIGINT NULL;

-- Create index for target_user_id
CREATE INDEX idx_comments_target_user ON comments (target_user_id);
