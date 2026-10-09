package com.picture_backend.model.dto.work;

import lombok.Data;

import java.io.Serializable;

@Data
public class FollowRequest implements Serializable {
    private Long userId;
}
