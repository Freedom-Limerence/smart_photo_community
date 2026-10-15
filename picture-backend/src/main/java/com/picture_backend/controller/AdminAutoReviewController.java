package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.AutoReviewService;
import com.picture_backend.service.RbacService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 机审（自动审核）管理与策略配置。
 *
 * <p>解决"每张图都要人工审"的瓶颈：规则命中的高危内容才转人工，
 * 低风险内容由机审直接放行，并可一键处理历史积压。</p>
 */
@RestController
@RequestMapping("/admin/console/auto-review")
public class AdminAutoReviewController {

    @Resource private UserService userService;
    @Resource private RbacService rbacService;
    @Resource private AutoReviewService autoReviewService;

    private User requireReview(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        if (!rbacService.hasPermission(user, "content:review")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少权限：content:review");
        }
        return user;
    }

    /** 当前策略 + 统计 */
    @GetMapping("/config")
    public BaseResponse<Map<String, Object>> config(HttpServletRequest request) {
        requireReview(request);
        Map<String, Object> data = autoReviewService.getConfig();
        data.putAll(autoReviewService.stats());
        return ResultUtils.success(data);
    }

    /** 更新策略（只接受 autoReview. 前缀的键） */
    @PostMapping("/config")
    public BaseResponse<Boolean> saveConfig(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireReview(request);
        autoReviewService.saveConfig(body);
        return ResultUtils.success(true);
    }

    /** 一键机审积压的待审核图片 */
    @PostMapping("/run")
    public BaseResponse<Map<String, Object>> run(@RequestParam(defaultValue = "100") Integer limit,
                                                 HttpServletRequest request) {
        requireReview(request);
        return ResultUtils.success(autoReviewService.reviewPending(limit == null ? 100 : limit));
    }

    /** 仅统计 */
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> stats(HttpServletRequest request) {
        requireReview(request);
        return ResultUtils.success(autoReviewService.stats());
    }
}
