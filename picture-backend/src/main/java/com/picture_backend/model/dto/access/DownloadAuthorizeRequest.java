package com.picture_backend.model.dto.access;

import lombok.Data;

import java.io.Serializable;

@Data
public class DownloadAuthorizeRequest implements Serializable {
    private String requestId;
    private Long pictureId;
    private Long workId;
}
