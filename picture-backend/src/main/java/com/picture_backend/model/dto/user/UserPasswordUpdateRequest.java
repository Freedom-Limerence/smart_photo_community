package com.picture_backend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/** 当前用户修改密码请求。 */
@Data
public class UserPasswordUpdateRequest implements Serializable {

    private String currentPassword;

    private String newPassword;

    private String confirmPassword;

    private static final long serialVersionUID = 1L;
}
