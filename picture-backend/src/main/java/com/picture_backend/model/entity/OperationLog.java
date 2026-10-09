package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 操作日志（只增不改）
 * @TableName operation_log
 */
@TableName(value = "operation_log")
@Data
public class OperationLog implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 操作用户 id
     */
    private Long userId;

    /**
     * 操作动作（如：审核通过）
     */
    private String action;

    /**
     * 所属模块（如：编目/展览/审核）
     */
    private String module;

    /**
     * 操作详情
     */
    private String detail;

    /**
     * 操作 ip
     */
    private String ip;

    /**
     * 耗时（毫秒）
     */
    private Long costMs;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
