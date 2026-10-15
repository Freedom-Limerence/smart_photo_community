-- V2026_10_21__review_log_and_work_review.sql
-- 1) 审核流水（谁在何时审了什么、改前改后）；2) 作品审核字段；3) 评论隐藏状态
CREATE TABLE IF NOT EXISTS review_log (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  targetType   VARCHAR(16)  NOT NULL COMMENT 'picture/work/comment',
  targetId     BIGINT       NOT NULL,
  action       VARCHAR(32)  NOT NULL COMMENT 'AUTO_PASS/MANUAL_PASS/REJECT/BAN/RESTORE/APPEAL_ACCEPT/HIDE/UNHIDE/AUTO_HIDE',
  fromStatus   VARCHAR(32)  DEFAULT NULL,
  toStatus     VARCHAR(32)  DEFAULT NULL,
  operatorId   BIGINT       DEFAULT NULL COMMENT '操作人（空=系统机审）',
  operatorType VARCHAR(16)  DEFAULT NULL COMMENT 'SYSTEM/ADMIN/AUTHOR',
  reason       VARCHAR(512) DEFAULT NULL,
  createTime   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_target (targetType, targetId, createTime),
  KEY idx_time (createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核流水';

SET @e := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='photo_work' AND COLUMN_NAME='reviewStatus');
SET @s := IF(@e=0, 'ALTER TABLE photo_work ADD COLUMN reviewStatus INT NOT NULL DEFAULT 1 COMMENT ''0待审/1通过/2拒绝''', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @e := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='photo_work' AND COLUMN_NAME='reviewMessage');
SET @s := IF(@e=0, 'ALTER TABLE photo_work ADD COLUMN reviewMessage VARCHAR(512) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @e := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='photo_work' AND COLUMN_NAME='reviewTime');
SET @s := IF(@e=0, 'ALTER TABLE photo_work ADD COLUMN reviewTime DATETIME NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @e := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='photo_work' AND COLUMN_NAME='reviewerId');
SET @s := IF(@e=0, 'ALTER TABLE photo_work ADD COLUMN reviewerId BIGINT NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;