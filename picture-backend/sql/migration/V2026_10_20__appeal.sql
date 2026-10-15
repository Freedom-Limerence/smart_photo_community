-- V2026_10_20__appeal.sql
-- 图片审核申诉：被拒绝的图片可申诉，受理后重新上架
CREATE TABLE IF NOT EXISTS appeal (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  pictureId    BIGINT       NOT NULL COMMENT '被拒绝的图片',
  userId       BIGINT       NOT NULL COMMENT '申诉人（须为图片作者）',
  reason       VARCHAR(512) NOT NULL COMMENT '申诉理由',
  status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED',
  handlerId    BIGINT       DEFAULT NULL,
  handleResult VARCHAR(512) DEFAULT NULL,
  handleTime   DATETIME     DEFAULT NULL,
  createTime   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status (status, createTime),
  KEY idx_user (userId, createTime),
  KEY idx_picture (pictureId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核申诉';