package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 系统公告 */
@Data
@TableName("announcement")
public class Announcement implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    /** INFO / WARNING / IMPORTANT */
    private String level;
    /** DRAFT / PUBLISHED / OFFLINE */
    private String status;
    private Long publisherId;
    private Long viewCount;
    private Date publishTime;
    private Date createTime;
    private Date updateTime;
}
