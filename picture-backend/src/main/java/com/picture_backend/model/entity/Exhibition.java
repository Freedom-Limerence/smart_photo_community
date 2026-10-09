package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 专题展览
 * @TableName exhibition
 */
@TableName(value = "exhibition")
@Data
public class Exhibition implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 展览标题
     */
    private String title;

    /**
     * 展览主题
     */
    private String theme;

    /**
     * 封面图 url
     */
    private String coverUrl;

    /**
     * 展览介绍
     */
    private String description;

    /**
     * 状态：0-草稿 1-已发布 2-已下线
     */
    private Integer status;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
