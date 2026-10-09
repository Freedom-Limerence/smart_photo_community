package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("download_record")
public class DownloadRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private String downloadToken;
    private Long userId;
    private Long workId;
    private Long pictureId;
    private Long spaceId;
    private Date quotaDate;
    private Integer quotaCost;
    private String result;
    private Date createTime;
}
