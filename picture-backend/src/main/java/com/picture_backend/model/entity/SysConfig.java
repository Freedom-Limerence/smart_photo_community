package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 系统配置（键值） */
@Data
@TableName("sys_config")
public class SysConfig implements Serializable {
    @TableId(type = IdType.INPUT)
    private String configKey;
    private String configValue;
    private String description;
    private Date updateTime;
}
