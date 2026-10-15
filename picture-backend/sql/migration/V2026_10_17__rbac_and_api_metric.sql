-- ============================================================
-- V2026_10_17__rbac_and_api_metric.sql
-- 任务三：RBAC（角色 / 权限 / 角色权限）+ 系统监控接口指标
-- 幂等：可重复执行（CREATE TABLE IF NOT EXISTS + INSERT IGNORE）
-- 说明：本库列名为 camelCase（map-underscore-to-camel-case: false）
-- ============================================================

-- 1. 角色
CREATE TABLE IF NOT EXISTS sys_role (
  roleCode    VARCHAR(32)  NOT NULL COMMENT '角色编码：admin/curator/user/banned',
  roleName    VARCHAR(64)  NOT NULL COMMENT '角色名称',
  description VARCHAR(255) DEFAULT NULL COMMENT '说明',
  sortOrder   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  isBuiltin   TINYINT      NOT NULL DEFAULT 1 COMMENT '是否内置（内置角色不允许删除）',
  createTime  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updateTime  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (roleCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

-- 2. 权限点
CREATE TABLE IF NOT EXISTS sys_permission (
  permCode    VARCHAR(64)  NOT NULL COMMENT '权限编码，如 content:review',
  permName    VARCHAR(64)  NOT NULL COMMENT '权限名称',
  module      VARCHAR(32)  NOT NULL COMMENT '所属模块',
  description VARCHAR(255) DEFAULT NULL,
  sortOrder   INT          NOT NULL DEFAULT 0,
  createTime  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (permCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限点';

-- 3. 角色-权限
CREATE TABLE IF NOT EXISTS sys_role_permission (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  roleCode    VARCHAR(32)  NOT NULL,
  permCode    VARCHAR(64)  NOT NULL,
  createTime  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_perm (roleCode, permCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

-- 4. 接口调用指标（系统监控，按小时聚合）
CREATE TABLE IF NOT EXISTS api_metric (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  bucketHour   DATETIME     NOT NULL COMMENT '小时桶（取整点）',
  uri          VARCHAR(255) NOT NULL COMMENT '接口路径（已归并数字 id）',
  method       VARCHAR(10)  NOT NULL,
  requestCount BIGINT       NOT NULL DEFAULT 0,
  errorCount   BIGINT       NOT NULL DEFAULT 0,
  totalCostMs  BIGINT       NOT NULL DEFAULT 0,
  maxCostMs    BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_metric (bucketHour, uri, method),
  KEY idx_bucket (bucketHour)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口调用指标（按小时）';

-- ============ 初始化数据（重复执行安全）============
INSERT IGNORE INTO sys_role (roleCode, roleName, description, sortOrder, isBuiltin) VALUES
 ('admin',   '超级管理员', '拥有全部权限',                     1, 1),
 ('curator', '策展人',     '负责内容审核与专题策展，无用户封禁与系统配置权限', 2, 1),
 ('user',    '普通用户',   '前台创作与互动',                   3, 1),
 ('banned',  '已封禁',     '禁止登录与互动',                   4, 1);

INSERT IGNORE INTO sys_permission (permCode, permName, module, sortOrder, description) VALUES
 ('stats:view',        '查看数据看板',   '统计', 10, '访问 /admin/dashboard'),
 ('content:review',    '内容审核',       '审核', 20, '查看待审图片与作品队列'),
 ('content:moderate',  '内容治理',       '审核', 30, '批量通过/拒绝图片，封禁/恢复作品'),
 ('picture:manage',    '图片管理',       '资产', 40, '管理站内全部图片'),
 ('space:manage',      '空间管理',       '资产', 50, '管理全部空间'),
 ('user:manage',       '用户管理',       '用户', 60, '查看与编辑用户资料'),
 ('user:ban',          '用户封禁',       '用户', 70, '封禁 / 解封账号'),
 ('exhibition:manage', '专题策展',       '内容', 80, '创建与发布专题展览'),
 ('log:view',          '操作日志',       '审计', 90, '查看后台操作审计日志'),
 ('rbac:manage',       '角色与权限',     '系统', 100, '维护角色权限矩阵与用户角色'),
 ('system:monitor',    '系统监控',       '系统', 110, '查看接口调用指标');

-- 超级管理员：全部权限
INSERT IGNORE INTO sys_role_permission (roleCode, permCode)
SELECT 'admin', permCode FROM sys_permission;

-- 策展人：审核 + 治理 + 图片/专题 + 看板/日志，**不含** user:ban / rbac:manage / system:monitor
INSERT IGNORE INTO sys_role_permission (roleCode, permCode) VALUES
 ('curator', 'stats:view'),
 ('curator', 'content:review'),
 ('curator', 'content:moderate'),
 ('curator', 'picture:manage'),
 ('curator', 'exhibition:manage'),
 ('curator', 'log:view');

-- 普通用户与封禁用户：无后台权限
