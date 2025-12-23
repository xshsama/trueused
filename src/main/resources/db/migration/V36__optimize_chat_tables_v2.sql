-- 1. 定义一个存储过程来安全删除索引
DROP PROCEDURE IF EXISTS drop_index_if_exists;

DELIMITER $$

CREATE PROCEDURE drop_index_if_exists()
BEGIN
    -- 检查索引是否存在 (适配老版本 MySQL)
    -- 注意：请将 'idx_sender' 和 'chat_messages' 替换为你实际的索引名和表名
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics 
        WHERE table_schema = DATABASE() 
        AND table_name = 'chat_messages' 
        AND index_name = 'idx_sender' -- 这里写你要删的索引名
    ) THEN
        ALTER TABLE chat_messages DROP INDEX idx_sender;
    END IF;

    -- 如果还有其他索引要删，可以复制上面的 IF 块继续写
    -- 例如删除 idx_receiver:
    /*
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics 
        WHERE table_schema = DATABASE() 
        AND table_name = 'chat_messages' 
        AND index_name = 'idx_receiver'
    ) THEN
        ALTER TABLE chat_messages DROP INDEX idx_receiver;
    END IF;
    */

END $$

DELIMITER ;

-- 2. 执行存储过程
CALL drop_index_if_exists();

-- 3. 清理存储过程
DROP PROCEDURE IF EXISTS drop_index_if_exists;