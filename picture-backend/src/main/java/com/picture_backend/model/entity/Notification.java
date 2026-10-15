package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 站内通知 */
@Data
@TableName("notification")
public class Notification implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** LIKE / FAVORITE / COMMENT / FOLLOW / SYSTEM */
    private String type;
    private String title;
    private String content;
    private Long senderId;
    /** work / picture / user / announcement */
    private String targetType;
    private Long targetId;
    private Integer isRead;
    private Date createTime;
}
