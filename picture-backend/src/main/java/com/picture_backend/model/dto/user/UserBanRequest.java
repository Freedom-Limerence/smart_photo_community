package com.picture_backend.model.dto.user;

import lombok.Data;

@Data
public class UserBanRequest {
    private Long id;
    private Boolean banned;
}
