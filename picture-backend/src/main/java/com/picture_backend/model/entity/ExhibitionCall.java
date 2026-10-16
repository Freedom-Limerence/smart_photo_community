package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 专题征稿启事（B2 活动·征稿） */
@Data
@TableName("exhibition_call")
public class ExhibitionCall implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联专题 exhibition.id */
    private Long exhibitionId;

    /** 征稿标题 */
    private String title;

    /** 征稿说明 */
    private String description;

    /** 截稿时间 */
    private Date deadline;

    /** 每人限投数 */
    private Integer maxSubmissionsPerUser;

    /** DRAFT / OPEN / CLOSED */
    private String status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;
}
