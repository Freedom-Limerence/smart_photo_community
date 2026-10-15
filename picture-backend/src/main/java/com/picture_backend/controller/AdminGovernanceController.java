package com.picture_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.model.entity.Announcement;
import com.picture_backend.model.entity.Report;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.AnnouncementService;
import com.picture_backend.service.RbacService;
import com.picture_backend.service.ReportService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端治理扩展：举报处理中心 + 公告管理。
 * 权限点：report:manage / announcement:manage（策展人亦可用）。
 */
@RestController
@RequestMapping("/admin/console")
public class AdminGovernanceController {

    @Resource private UserService userService;
    @Resource private RbacService rbacService;
    @Resource private ReportService reportService;
    @Resource private AnnouncementService announcementService;

    private User require(HttpServletRequest request, String permCode) {
        User user = userService.getLoginUser(request);
        if (!rbacService.hasPermission(user, permCode)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少权限：" + permCode);
        }
        return user;
    }

    /* ================= 举报中心 ================= */

    @GetMapping("/reports")
    public BaseResponse<Map<String, Object>> reports(@RequestParam(required = false) String status,
                                                     @RequestParam(defaultValue = "1") Long current,
                                                     @RequestParam(defaultValue = "10") Long size,
                                                     HttpServletRequest request) {
        require(request, "report:manage");
        Page<Report> page = reportService.page(status, current, size);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("pendingCount", reportService.pendingCount());
        return ResultUtils.success(data);
    }

    @PostMapping("/reports/handle")
    public BaseResponse<Boolean> handleReport(@RequestBody HandleRequest body, HttpServletRequest request) {
        User handler = require(request, "report:manage");
        if (body == null || body.getId() == null || body.getAction() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean resolved = "RESOLVE".equalsIgnoreCase(body.getAction());
        if (!resolved && !"REJECT".equalsIgnoreCase(body.getAction())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的处理动作");
        }
        return ResultUtils.success(reportService.handle(handler, body.getId(), resolved, body.getResult()));
    }

    /* ================= 公告管理 ================= */

    @GetMapping("/announcements")
    public BaseResponse<Page<Announcement>> announcements(@RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") Long current,
                                                          @RequestParam(defaultValue = "10") Long size,
                                                          HttpServletRequest request) {
        require(request, "announcement:manage");
        return ResultUtils.success(announcementService.page(status, current, size));
    }

    @PostMapping("/announcements/save")
    public BaseResponse<Long> saveAnnouncement(@RequestBody Announcement body, HttpServletRequest request) {
        User publisher = require(request, "announcement:manage");
        return ResultUtils.success(announcementService.save(publisher, body));
    }

    @PostMapping("/announcements/status")
    public BaseResponse<Boolean> changeAnnouncementStatus(@RequestBody StatusRequest body, HttpServletRequest request) {
        User publisher = require(request, "announcement:manage");
        if (body == null || body.getId() == null || body.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(announcementService.changeStatus(publisher, body.getId(), body.getStatus(),
                !Boolean.FALSE.equals(body.getBroadcast())));
    }

    @Data
    public static class HandleRequest {
        private Long id;
        /** RESOLVE / REJECT */
        private String action;
        private String result;
    }

    @Data
    public static class StatusRequest {
        private Long id;
        /** DRAFT / PUBLISHED / OFFLINE */
        private String status;
        /** 是否推送站内通知给全体用户（默认 true） */
        private Boolean broadcast;
    }
}
