package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 作品收藏
 */
@Data
@TableName("work_favorite")
public class WorkFavorite implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workId;
    private Long userId;

    /** 所属收藏夹，空表示未分组 */
    private Long folderId;
    private Date createTime;
}