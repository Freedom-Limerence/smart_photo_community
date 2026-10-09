package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 收藏-图片关联
 * @TableName collection_picture
 */
@TableName(value = "collection_picture")
@Data
public class CollectionPicture implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 收藏夹 id
     */
    private Long collectionId;

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 收藏时间
     */
    private Date createTime;

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
