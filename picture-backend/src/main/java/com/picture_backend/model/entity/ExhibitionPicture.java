package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 展览-作品关联
 * @TableName exhibition_picture
 */
@TableName(value = "exhibition_picture")
@Data
public class ExhibitionPicture implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 展览 id
     */
    private Long exhibitionId;

    /**
     * 图片（作品）id
     */
    private Long pictureId;

    /**
     * 展品说明
     */
    private String caption;

    /**
     * 排序号（越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 创建时间
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
