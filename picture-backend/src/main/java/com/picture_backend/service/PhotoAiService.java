package com.picture_backend.service;

import com.picture_backend.model.entity.PhotoAiTask;
import com.picture_backend.model.entity.PhotoAssetMeta;

import java.util.List;
import java.util.Map;

public interface PhotoAiService {
    PhotoAiTask catalog(Long pictureId, Long userId);
    PhotoAiTask startExifTask(Long pictureId, Long userId);
    PhotoAiTask retryTask(Long taskId, Long userId);
    PhotoAiTask cancelTask(Long taskId, Long userId);
    List<PhotoAiTask> listMyTasks(Long userId, int current, int size, String status);
    PhotoAssetMeta confirm(Long pictureId, Long userId, PhotoAssetMeta update);
    PhotoAssetMeta getMeta(Long pictureId);
    PhotoAiTask getTask(Long taskId);
    List<Long> search(String query, int topK, Long userId);
    List<Map<String, Object>> searchWithEvidence(String query, int topK, Long userId);
    Map<String, Object> similarGroups(Long pictureId, Long userId, Integer timeWindowSeconds, Double threshold);
}
