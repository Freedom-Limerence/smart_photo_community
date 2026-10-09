package com.picture_backend.model.dto.collection;

import lombok.Data;

import java.io.Serializable;

/**
 * 收藏/取消收藏图片请求
 */
@Data
public class CollectionPictureRequest implements Serializable {

    /**
     * 收藏夹 id
     */
    private Long collectionId;

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
