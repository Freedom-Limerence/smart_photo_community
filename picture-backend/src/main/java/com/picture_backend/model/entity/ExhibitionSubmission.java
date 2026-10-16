package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 专题征稿投稿（B2 活动·征稿） */
@Data
@TableName("exhibition_submission")
public class ExhibitionSubmission implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联征稿 exhibition_call.id */
    private Long callId;

    /** 投稿用户 */
    private Long userId;

    /** 投稿作品 photo_work.id */
    private Long workId;

    /** PENDING / ACCEPTED / REJECTED */
    private String status;

    /** 投稿简述 */
    private String caption;

    /** 评审人 */
    private Long reviewerId;

    /** 评审时间 */
    private Date reviewTime;

    private Date createTime;

    private Date updateTime;
}
