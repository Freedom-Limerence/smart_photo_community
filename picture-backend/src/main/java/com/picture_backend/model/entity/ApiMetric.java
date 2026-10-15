package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 接口调用指标（按小时聚合） */
@Data
@TableName("api_metric")
public class ApiMetric implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Date bucketHour;
    private String uri;
    private String method;
    private Long requestCount;
    private Long errorCount;
    private Long totalCostMs;
    private Long maxCostMs;
}
