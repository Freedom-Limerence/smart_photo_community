package com.picture_backend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 投稿列表视图（供管理端投稿管理页与用户端「我的投稿」使用） */
@Data
public class ExhibitionSubmissionVO implements Serializable {

    private Long id;
    private Long callId;
    private Long exhibitionId;

    private Long userId;
    private String userName;
    private String userAvatar;

    private Long workId;
    private String workTitle;
    private String workThumb;
    private Long workLikeCount;

    /** PENDING / ACCEPTED / REJECTED */
    private String status;
    private String caption;

    private Long reviewerId;
    private String reviewerName;
    private Date reviewTime;

    private Date createTime;
}
