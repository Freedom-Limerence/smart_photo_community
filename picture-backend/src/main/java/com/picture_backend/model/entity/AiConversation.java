package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 助手会话
 */
@Data
@TableName("ai_conversation")
public class AiConversation implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private Date lastMessageAt;
    private Integer messageCount;
    private Integer isDelete;
    private Date createTime;
    private Date updateTime;
}
