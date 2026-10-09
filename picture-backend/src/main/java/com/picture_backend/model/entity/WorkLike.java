package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("work_like")
public class WorkLike implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workId;
    private Long userId;
    private Date createTime;
}
