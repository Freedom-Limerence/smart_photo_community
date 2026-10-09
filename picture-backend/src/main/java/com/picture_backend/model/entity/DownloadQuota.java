package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("download_quota")
public class DownloadQuota implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Date quotaDate;
    private Integer quotaLimit;
    private Integer usedCount;
    private String ruleVersion;
    private Date createTime;
    private Date updateTime;
}
