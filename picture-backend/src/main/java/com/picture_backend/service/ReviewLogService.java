package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.model.entity.ReviewLog;
import com.picture_backend.model.entity.User;

import java.util.List;
import java.util.Map;

/** 审核流水 */
public interface ReviewLogService {

    void record(String targetType, Long targetId, String action, Object fromStatus, Object toStatus, User operator, String reason);

    void recordSystem(String targetType, Long targetId, String action, Object fromStatus, Object toStatus, String reason);

    Page<ReviewLog> page(String targetType, Long targetId, long current, long size);

    List<Map<String, Object>> decorate(List<ReviewLog> logs);
}
