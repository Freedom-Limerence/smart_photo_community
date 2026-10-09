package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("photo_vector")
public class PhotoVector implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pictureId;
    private String content;
    private String keywords;
    private String modelVersion;
    private Date createTime;
    private Date updateTime;
}
