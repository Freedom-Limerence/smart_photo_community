package com.picture_backend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureBatchUploadItem implements Serializable {
    private String sourceUrl;
    private String name;
    private Long pictureId;
    private String status;
    private String errorMessage;
}
