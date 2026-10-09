package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("photo_ai_task")
public class PhotoAiTask implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pictureId;
    private Long workId;
    private String taskType;
    private String status;
    private String provider;
    private String providerVersion;
    private String requestId;
    private Integer retryCount;
    private String resultJson;
    private String errorMessage;
    private Date startedAt;
    private Date finishedAt;
    private Date createTime;
    private Date updateTime;
}
