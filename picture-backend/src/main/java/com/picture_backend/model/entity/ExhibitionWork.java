package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("exhibition_work")
public class ExhibitionWork implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long exhibitionId;
    private Long workId;
    private String caption;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
}
