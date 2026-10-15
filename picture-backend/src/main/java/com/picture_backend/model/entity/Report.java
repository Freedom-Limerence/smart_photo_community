package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 内容举报 */
@Data
@TableName("report")
public class Report implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reporterId;
    /** work / picture / comment / user */
    private String targetType;
    private Long targetId;
    private String reason;
    private String detail;
    /** PENDING / RESOLVED / REJECTED */
    private String status;
    private Long handlerId;
    private String handleResult;
    private Date handleTime;
    private Date createTime;
}
