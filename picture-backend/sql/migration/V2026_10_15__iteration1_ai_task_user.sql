-- 迭代 1（阶段 2 收口）：AI 任务归属字段 + 索引
-- 可重复执行：先查 information_schema，再决定是否变更
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'photo_ai_task' AND COLUMN_NAME = 'userId');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE photo_ai_task ADD COLUMN userId BIGINT NULL COMMENT ''任务发起人''',
    'SELECT ''column userId already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'photo_ai_task' AND INDEX_NAME = 'idx_ai_task_user');
SET @sql2 = IF(@idx_exists = 0,
    'CREATE INDEX idx_ai_task_user ON photo_ai_task (userId, status)',
    'SELECT ''index idx_ai_task_user already exists''');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
