package com.picture_backend.model.dto.access;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class InviteCreateRequest implements Serializable {
    private Long spaceId;
    private String spaceRole;
    private Integer maxUses;
    private Date expireTime;
}
