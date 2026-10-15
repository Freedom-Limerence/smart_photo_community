-- V2026_10_22__comment_like.sql
-- 评论点赞（回复复用 work_comment.parentId，无需新表）
CREATE TABLE IF NOT EXISTS work_comment_like (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  commentId  BIGINT   NOT NULL,
  userId     BIGINT   NOT NULL,
  createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_comment_user (commentId, userId),
  KEY idx_comment (commentId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞';
SET @e := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='work_comment' AND COLUMN_NAME='likeCount');
SET @s := IF(@e=0, 'ALTER TABLE work_comment ADD COLUMN likeCount BIGINT NOT NULL DEFAULT 0', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;