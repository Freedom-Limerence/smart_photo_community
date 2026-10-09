package com.picture_backend.service;

import com.picture_backend.model.entity.PhotoAiTask;
import com.picture_backend.model.entity.PhotoAssetMeta;

import java.util.List;

public interface PhotoAiService {
    PhotoAiTask catalog(Long pictureId, Long userId);
    PhotoAssetMeta confirm(Long pictureId, Long userId, PhotoAssetMeta update);
    PhotoAssetMeta getMeta(Long pictureId);
    PhotoAiTask getTask(Long taskId);
    List<Long> search(String query, int topK, Long userId);
}
