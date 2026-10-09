package com.picture_backend.model.dto.collection;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新收藏夹请求
 */
@Data
public class CollectionUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 收藏夹名称
     */
    private String name;

    /**
     * 收藏夹描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
