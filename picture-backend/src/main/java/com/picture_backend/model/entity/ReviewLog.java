package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 审核流水：谁在何时审了什么、改前改后 */
@Data
@TableName("review_log")
public class ReviewLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** picture / work / comment */
    private String targetType;
    private Long targetId;
    /** AUTO_PASS / MANUAL_PASS / REJECT / BAN / RESTORE / APPEAL_ACCEPT / HIDE / UNHIDE / AUTO_HIDE */
    private String action;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    /** SYSTEM / ADMIN / AUTHOR */
    private String operatorType;
    private String reason;
    private Date createTime;
}
