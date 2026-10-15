package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.manager.metric.ApiMetricRecorder;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.RbacService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统监控：接口调用量 / 耗时 / 错误率（基于 api_metric 小时聚合表）。
 */
@RestController
@RequestMapping("/admin/console")
public class AdminMonitorController {

    @Resource private UserService userService;
    @Resource private RbacService rbacService;
    @Resource private ApiMetricRecorder apiMetricRecorder;

    private User requireMonitor(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        if (!rbacService.hasPermission(user, "system:monitor")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少权限：system:monitor");
        }
        return user;
    }

    /** 指标总览：Top 接口 + 小时趋势 + 汇总 */
    @GetMapping("/metrics")
    public BaseResponse<Map<String, Object>> metrics(@RequestParam(defaultValue = "24") Integer hours,
                                                     @RequestParam(defaultValue = "20") Integer limit,
                                                     HttpServletRequest request) {
        requireMonitor(request);
        List<Map<String, Object>> summary = apiMetricRecorder.summary(hours, Math.max(1, Math.min(limit, 100)));
        List<Map<String, Object>> hourly = apiMetricRecorder.hourly(hours);

        java.util.Map<String, Object> totals = apiMetricRecorder.totals(hours);
        long totalRequestsAll = toLong(totals.get("requestCount"));
        long totalErrorsAll = toLong(totals.get("errorCount"));
        long totalCostAll = toLong(totals.get("totalCostMs"));
        long maxCostAll = toLong(totals.get("maxCostMs"));
        long totalRequests = 0L;
        long totalErrors = 0L;
        long totalCost = 0L;
        long maxCost = 0L;
        for (Map<String, Object> row : summary) {
            totalRequests += toLong(row.get("requestCount"));
            totalErrors += toLong(row.get("errorCount"));
            totalCost += toLong(row.get("totalCostMs"));
            maxCost = Math.max(maxCost, toLong(row.get("maxCostMs")));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hours", hours);
        data.put("summary", summary);
        data.put("hourly", hourly);
        data.put("totalRequests", totalRequestsAll);
        data.put("totalErrors", totalErrorsAll);
        data.put("avgCostMs", totalRequestsAll == 0 ? 0 : Math.round((double) totalCostAll / totalRequestsAll));
        data.put("maxCostMs", maxCostAll);
        data.put("errorRate", totalRequestsAll == 0 ? 0D : Math.round(totalErrorsAll * 10000D / totalRequestsAll) / 100D);
        data.putAll(apiMetricRecorder.pending());
        return ResultUtils.success(data);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
