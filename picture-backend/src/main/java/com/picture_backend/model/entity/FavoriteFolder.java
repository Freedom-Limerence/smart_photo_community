package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 收藏夹分组（P0 交互补齐） */
@Data
@TableName("favorite_folder")
public class FavoriteFolder implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String description;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;
}
