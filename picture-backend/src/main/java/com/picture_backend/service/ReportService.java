package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.model.entity.Report;
import com.picture_backend.model.entity.User;

public interface ReportService {
    Long submit(User reporter, String targetType, Long targetId, String reason, String detail);

    Page<Report> page(String status, long current, long size);

    long pendingCount();

    boolean handle(User handler, Long reportId, boolean resolved, String result);
}
