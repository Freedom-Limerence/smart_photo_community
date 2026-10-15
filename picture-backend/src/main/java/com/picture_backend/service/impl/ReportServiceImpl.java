package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.ReportMapper;
import com.picture_backend.model.entity.Report;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.NotificationService;
import com.picture_backend.service.ReportService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Resource private ReportMapper reportMapper;
    @Resource private NotificationService notificationService;

    @Override
    public Long submit(User reporter, String targetType, Long targetId, String reason, String detail) {
        if (reporter == null || targetId == null || !StringUtils.hasText(targetType) || !StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报信息不完整");
        }
        List<String> allowed = List.of("work", "picture", "comment", "user");
        if (!allowed.contains(targetType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的举报类型");
        }
        long duplicated = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporter.getId())
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId)
                .eq(Report::getStatus, "PENDING"));
        if (duplicated > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "你已经举报过该内容，我们正在处理");
        }
        Report report = new Report();
        report.setReporterId(reporter.getId());
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setDetail(detail);
        report.setStatus("PENDING");
        reportMapper.insert(report);
        return report.getId();
    }

    @Override
    public Page<Report> page(String status, long current, long size) {
        return reportMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Report>()
                .eq(StringUtils.hasText(status), Report::getStatus, status)
                .orderByAsc(Report::getStatus)
                .orderByDesc(Report::getCreateTime));
    }

    @Override
    public long pendingCount() {
        return reportMapper.selectCount(new LambdaQueryWrapper<Report>().eq(Report::getStatus, "PENDING"));
    }

    @Override
    public boolean handle(User handler, Long reportId, boolean resolved, String result) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "举报不存在");
        }
        if (!"PENDING".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该举报已处理");
        }
        report.setStatus(resolved ? "RESOLVED" : "REJECTED");
        report.setHandlerId(handler == null ? null : handler.getId());
        report.setHandleResult(result);
        report.setHandleTime(new Date());
        reportMapper.updateById(report);
        notificationService.notifyReportResult(report.getReporterId(), report.getId(), resolved, result);
        return true;
    }
}
