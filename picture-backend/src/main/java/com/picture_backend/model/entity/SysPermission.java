package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 权限点 */
@Data
@TableName("sys_permission")
public class SysPermission implements Serializable {
    @TableId(type = IdType.INPUT)
    private String permCode;
    private String permName;
    private String module;
    private String description;
    private Integer sortOrder;
    private Date createTime;
}
