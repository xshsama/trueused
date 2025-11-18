-- 将 users.avatar_url 字段长度统一为 255 以匹配实体与校验
ALTER TABLE users
  MODIFY COLUMN avatar_url VARCHAR(255) NULL;
