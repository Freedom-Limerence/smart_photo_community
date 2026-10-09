package com.picture_backend.model.dto.exhibition;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 更新展览请求（管理端）
 */
@Data
public class ExhibitionUpdateRequest implements Serializable {

    /**
     * id
     */
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

    private static final long serialVersionUID = 1L;
}
