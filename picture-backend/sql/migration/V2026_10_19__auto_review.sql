-- V2026_10_19__auto_review.sql
-- 机审（自动审核）策略配置：机审 + 人审分层，降低逐张人工审核成本
CREATE TABLE IF NOT EXISTS sys_config (
  configKey   VARCHAR(64)  NOT NULL COMMENT '配置键',
  configValue VARCHAR(512) NOT NULL COMMENT '配置值（字符串，按需解析）',
  description VARCHAR(255) DEFAULT NULL,
  updateTime  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (configKey)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置（键值）';

INSERT IGNORE INTO sys_config (configKey, configValue, description) VALUES
 ('autoReview.enabled',            'true',  '是否启用机审'),
 ('autoReview.minWidth',           '400',   '低于该宽度的图片转人工'),
 ('autoReview.minHeight',          '400',   '低于该高度的图片转人工'),
 ('autoReview.minPassRate',        '0.8',   '作者历史通过率下限（0-1）'),
 ('autoReview.minHistory',         '3',     '至少需要多少张已审图片才信任该作者'),
 ('autoReview.burstWindowMinutes', '10',    '突发上传统计窗口（分钟）'),
 ('autoReview.burstLimit',         '15',    '窗口内上传超过该数量转人工'),
 ('autoReview.bannedWords',        '涉政,赌博,色情,暴力,违禁,代刷,诈骗', '名称/简介/标签命中的风险词（逗号分隔）');
