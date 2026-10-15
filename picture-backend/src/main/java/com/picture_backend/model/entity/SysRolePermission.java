package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 角色-权限关联 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleCode;
    private String permCode;
    private Date createTime;
}
