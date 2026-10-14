-- 任务一（AI 智能助手）：会话与消息表
-- 可重复执行：新增表使用 IF NOT EXISTS，索引内联在表定义中
CREATE TABLE IF NOT EXISTS ai_conversation (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话 id',
  userId        BIGINT       NOT NULL COMMENT '归属用户',
  title         VARCHAR(128) NULL COMMENT '会话标题（取首条问题前 30 字）',
  lastMessageAt DATETIME     NULL COMMENT '最后一条消息时间',
  messageCount  INT          NOT NULL DEFAULT 0 COMMENT '消息条数',
  isDelete      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  createTime    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updateTime    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_ai_conversation_user (userId, lastMessageAt)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 助手会话';

CREATE TABLE IF NOT EXISTS ai_message (
  id             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '消息 id',
  conversationId BIGINT   NOT NULL COMMENT '所属会话',
  userId         BIGINT   NOT NULL COMMENT '归属用户',
  role           VARCHAR(16) NOT NULL COMMENT 'user / assistant / system',
  content        TEXT     NULL COMMENT '消息内容',
  referencesJson TEXT     NULL COMMENT '引用卡片 JSON',
  toolCallsJson  TEXT     NULL COMMENT '工具调用轨迹 JSON',
  provider       VARCHAR(32) NULL COMMENT '生成来源：LOCAL_TEMPLATE / DASHSCOPE',
  latencyMs      BIGINT   NULL COMMENT '耗时（毫秒）',
  createTime     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_ai_message_conversation (conversationId, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 助手消息';
