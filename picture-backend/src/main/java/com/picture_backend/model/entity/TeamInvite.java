package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("team_invite")
public class TeamInvite implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spaceId;
    @JsonIgnore
    private String codeHash;
    private String spaceRole;
    private Integer maxUses;
    private Integer usedCount;
    private Date expireTime;
    private String status;
    private Long createUserId;
    private Date createTime;
    private Date updateTime;
}
