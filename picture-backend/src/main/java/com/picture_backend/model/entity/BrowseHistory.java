package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 浏览历史（只增不改）
 * @TableName browse_history
 */
@TableName(value = "browse_history")
@Data
public class BrowseHistory implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 浏览时间
     */
    private Date browseTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
