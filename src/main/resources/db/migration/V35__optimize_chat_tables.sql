-- ==========================================
-- V35: Optimize Chat Tables (Idempotent Script)
-- ==========================================

-- 1. Safe FK Drop
DROP PROCEDURE IF EXISTS drop_fk_if_exists;
DELIMITER $$
CREATE PROCEDURE drop_fk_if_exists(IN tableName VARCHAR(64), IN constraintName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT * FROM information_schema.table_constraints
        WHERE table_name = tableName AND constraint_name = constraintName AND table_schema = DATABASE()
    ) THEN
        SET @s = CONCAT('ALTER TABLE ', tableName, ' DROP FOREIGN KEY ', constraintName);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL drop_fk_if_exists('conversations', 'fk_conversations_participant1');
CALL drop_fk_if_exists('conversations', 'fk_conversations_participant2');
CALL drop_fk_if_exists('conversations', 'fk_conversations_last_message');
CALL drop_fk_if_exists('messages', 'fk_messages_conversations');
CALL drop_fk_if_exists('messages', 'fk_messages_sender');
CALL drop_fk_if_exists('messages', 'fk_messages_receiver');
DROP PROCEDURE drop_fk_if_exists;
-- V35 has been superseded by V36__optimize_chat_tables_v2.sql
-- The original V35 contained complex stored-procedure based operations that
-- caused parsing issues for Flyway (DELIMITER usage). To keep schema
-- history clean and avoid parser errors, this migration is now a no-op
-- placeholder. Please see V36__optimize_chat_tables_v2.sql for the safe
-- implementation of the chat-tables optimization.
