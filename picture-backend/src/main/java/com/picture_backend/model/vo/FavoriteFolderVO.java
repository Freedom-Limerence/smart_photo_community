package com.picture_backend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 收藏夹视图（含作品数量） */
@Data
public class FavoriteFolderVO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Date createTime;

    /** 该收藏夹内的作品数量 */
    private Long workCount;
}
