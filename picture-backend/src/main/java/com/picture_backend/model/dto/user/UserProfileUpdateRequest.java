package com.picture_backend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/** 当前用户编辑资料请求。 */
@Data
public class UserProfileUpdateRequest implements Serializable {

    private String userName;

    private String userAvatar;

    private String userProfile;

    private static final long serialVersionUID = 1L;
}
