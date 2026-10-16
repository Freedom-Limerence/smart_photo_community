-- V2026_10_24__favorite_folder.sql
-- P0 收藏夹分组：新建 favorite_folder，并为 work_favorite 增加 folderId

CREATE TABLE IF NOT EXISTS favorite_folder (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  userId      BIGINT       NOT NULL COMMENT '所属用户',
  name        VARCHAR(64)  NOT NULL COMMENT '收藏夹名称',
  description VARCHAR(255) NULL COMMENT '简介',
  sortOrder   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  createTime  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updateTime  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  isDelete    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_user (userId),
  UNIQUE KEY uk_user_name (userId, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏夹分组';

SET @e := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='work_favorite' AND COLUMN_NAME='folderId');
SET @s := IF(@e=0, 'ALTER TABLE work_favorite ADD COLUMN folderId BIGINT NULL COMMENT ''所属收藏夹，空表示未分组''', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @e2 := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='work_favorite' AND INDEX_NAME='idx_user_folder');
SET @s2 := IF(@e2=0, 'ALTER TABLE work_favorite ADD INDEX idx_user_folder (userId, folderId)', 'DO 0');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;