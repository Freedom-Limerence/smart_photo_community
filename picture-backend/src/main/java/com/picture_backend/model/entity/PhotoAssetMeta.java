package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("photo_asset_meta")
public class PhotoAssetMeta implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pictureId;
    private String exifJson;
    private String cameraMake;
    private String cameraModel;
    private String lensModel;
    private Date shootTime;
    private String shootLocation;
    private String orientation;
    private String dominantColor;
    private String subjectTags;
    private String sceneTags;
    private String lightTags;
    private String compositionTags;
    private Double qualityScore;
    private Integer confirmed;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
