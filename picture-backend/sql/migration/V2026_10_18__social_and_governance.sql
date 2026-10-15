-- ============================================================
-- V2026_10_18__social_and_governance.sql
-- 迭代 3：通知中心 + 内容举报 + 系统公告；并补充两个后台权限点
-- 幂等：可重复执行
-- ============================================================

-- 1. 通知（点赞/收藏/评论/关注/系统公告）
CREATE TABLE IF NOT EXISTS notification (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  userId      BIGINT       NOT NULL COMMENT '接收者',
  type        VARCHAR(32)  NOT NULL COMMENT 'LIKE/FAVORITE/COMMENT/FOLLOW/SYSTEM',
  title       VARCHAR(128) NOT NULL,
  content     VARCHAR(512) DEFAULT NULL,
  senderId    BIGINT       DEFAULT NULL COMMENT '触发者',
  targetType  VARCHAR(32)  DEFAULT NULL COMMENT 'work/picture/user/announcement',
  targetId    BIGINT       DEFAULT NULL,
  isRead      TINYINT      NOT NULL DEFAULT 0,
  createTime  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_read (userId, isRead, createTime),
  KEY idx_create (createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';

-- 2. 举报
CREATE TABLE IF NOT EXISTS report (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  reporterId   BIGINT       NOT NULL,
  targetType   VARCHAR(32)  NOT NULL COMMENT 'work/picture/comment/user',
  targetId     BIGINT       NOT NULL,
  reason       VARCHAR(64)  NOT NULL COMMENT '理由分类',
  detail       VARCHAR(512) DEFAULT NULL,
  status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RESOLVED/REJECTED',
  handlerId    BIGINT       DEFAULT NULL,
  handleResult VARCHAR(512) DEFAULT NULL,
  handleTime   DATETIME     DEFAULT NULL,
  createTime   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status (status, createTime),
  KEY idx_target (targetType, targetId),
  UNIQUE KEY uk_reporter_target (reporterId, targetType, targetId, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容举报';

-- 3. 公告
CREATE TABLE IF NOT EXISTS announcement (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  title       VARCHAR(128) NOT NULL,
  content     TEXT         NOT NULL,
  level       VARCHAR(16)  NOT NULL DEFAULT 'INFO' COMMENT 'INFO/WARNING/IMPORTANT',
  status      VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
  publisherId BIGINT       DEFAULT NULL,
  viewCount   BIGINT       NOT NULL DEFAULT 0,
  publishTime DATETIME     DEFAULT NULL,
  createTime  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updateTime  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status_time (status, publishTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告';

-- 4. 新增权限点：举报处理、公告管理（内容运营类，策展人也应有）
INSERT IGNORE INTO sys_permission (permCode, permName, module, sortOrder, description) VALUES
 ('report:manage',     '举报处理', '审核', 35, '查看与处置用户举报'),
 ('announcement:manage','公告管理', '内容', 85, '发布与下线系统公告');

INSERT IGNORE INTO sys_role_permission (roleCode, permCode) VALUES
 ('admin', 'report:manage'),
 ('admin', 'announcement:manage'),
 ('curator', 'report:manage'),
 ('curator', 'announcement:manage');
