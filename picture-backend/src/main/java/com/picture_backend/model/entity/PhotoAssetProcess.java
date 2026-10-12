package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 图片上传及后处理状态，独立于原 picture 表。 */
@Data
@TableName("photo_asset_process")
public class PhotoAssetProcess implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private Long pictureId;
    private Long userId;
    private Long spaceId;
    private String sourceType;
    private String status;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;
}
