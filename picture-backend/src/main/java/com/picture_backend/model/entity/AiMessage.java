package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 助手消息
 */
@Data
@TableName("ai_message")
public class AiMessage implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long userId;
    /** user / assistant / system */
    private String role;
    private String content;
    /** 引用卡片 JSON（图片/摄影师/专题） */
    private String referencesJson;
    /** 工具调用轨迹 JSON */
    private String toolCallsJson;
    /** LOCAL_TEMPLATE / DASHSCOPE */
    private String provider;
    private Long latencyMs;
    private Date createTime;
}
