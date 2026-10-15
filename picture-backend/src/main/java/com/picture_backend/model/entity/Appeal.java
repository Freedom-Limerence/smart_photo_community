package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 审核申诉 */
@Data
@TableName("appeal")
public class Appeal implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pictureId;
    private Long userId;
    private String reason;
    /** PENDING / ACCEPTED / REJECTED */
    private String status;
    private Long handlerId;
    private String handleResult;
    private Date handleTime;
    private Date createTime;
}
