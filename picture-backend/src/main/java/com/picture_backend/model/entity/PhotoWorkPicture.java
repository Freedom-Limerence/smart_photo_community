package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("photo_work_picture")
public class PhotoWorkPicture implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workId;
    private Long pictureId;
    private Integer sortOrder;
    private Date createTime;
}
