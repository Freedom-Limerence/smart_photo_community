package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 系统角色 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {
    @TableId(type = IdType.INPUT)
    private String roleCode;
    private String roleName;
    private String description;
    private Integer sortOrder;
    private Integer isBuiltin;
    private Date createTime;
    private Date updateTime;
}
