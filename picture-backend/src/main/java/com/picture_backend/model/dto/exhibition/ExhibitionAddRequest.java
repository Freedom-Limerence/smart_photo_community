package com.picture_backend.model.dto.exhibition;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 创建展览请求
 */
@Data
public class ExhibitionAddRequest implements Serializable {

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
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    private static final long serialVersionUID = 1L;
}
