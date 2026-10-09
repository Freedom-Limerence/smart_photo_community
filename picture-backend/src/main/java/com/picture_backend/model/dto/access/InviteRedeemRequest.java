package com.picture_backend.model.dto.access;

import lombok.Data;

import java.io.Serializable;

@Data
public class InviteRedeemRequest implements Serializable {
    private String code;
}
