-- V2026_10_23__exhibition_call_and_submission.sql
-- B2 活动·征稿：征稿启事 + 用户投稿
-- 注意：本项目 column 命名约定为 camelCase（mybatis-plus map-underscore-to-camel-case: false）

CREATE TABLE IF NOT EXISTS exhibition_call (
  id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  exhibitionId          BIGINT       NOT NULL COMMENT '关联专题 exhibition.id',
  title                 VARCHAR(128) NOT NULL COMMENT '征稿标题',
  description           TEXT         NULL COMMENT '征稿说明',
  deadline              DATETIME     NULL COMMENT '截稿时间',
  maxSubmissionsPerUser INT          NOT NULL DEFAULT 1 COMMENT '每人限投数',
  status                VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/OPEN/CLOSED',
  createTime            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updateTime            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  isDelete              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_exhibition_status (exhibitionId, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专题征稿启事';

CREATE TABLE IF NOT EXISTS exhibition_submission (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  callId      BIGINT      NOT NULL COMMENT '关联征稿 exhibition_call.id',
  userId      BIGINT      NOT NULL COMMENT '投稿用户',
  workId      BIGINT      NOT NULL COMMENT '投稿作品 photo_work.id',
  status      VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED',
  caption     VARCHAR(512) NULL COMMENT '投稿简述',
  reviewerId  BIGINT      NULL COMMENT '评审人',
  reviewTime  DATETIME    NULL COMMENT '评审时间',
  createTime  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updateTime  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_call_user_work (callId, userId, workId),
  KEY idx_call_status (callId, status),
  KEY idx_user (userId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专题征稿投稿';