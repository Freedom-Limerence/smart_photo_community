package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.picture_backend.model.vo.work.WorkCommentUserVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("work_comment")
public class WorkComment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workId;
    private Long userId;
    private Long parentId;
    private String content;
    private String status;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDelete;
    @TableField(exist = false)
    private WorkCommentUserVO user;
}
