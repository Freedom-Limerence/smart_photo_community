package com.picture_backend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class PictureBatchUploadResult implements Serializable {
    private Integer requestedCount;
    private Integer successCount;
    private Integer failureCount;
    private List<PictureBatchUploadItem> items = new ArrayList<>();
}
