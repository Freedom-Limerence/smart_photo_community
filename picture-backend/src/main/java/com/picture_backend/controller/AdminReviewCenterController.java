package com.picture_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.WorkCommentMapper;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.entity.WorkComment;
import com.picture_backend.service.NotificationService;
import com.picture_backend.service.RbacService;
import com.picture_backend.service.ReviewLogService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审核中心扩展：审核流水 + 评论治理。
 * 权限点 content:review（查看/处理评论）、log:view（审核流水）。
 */
@RestController
@RequestMapping("/admin/console")
public class AdminReviewCenterController {

    @Resource private UserService userService;
    @Resource private RbacService rbacService;
    @Resource private ReviewLogService reviewLogService;
    @Resource private WorkCommentMapper workCommentMapper;
    @Resource private NotificationService notificationService;

    private User require(HttpServletRequest request, String perm) {
        User user = userService.getLoginUser(request);
        if (!rbacService.hasPermission(user, perm)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少权限：" + perm);
        }
        return user;
    }

    /** 审核流水：可按内容类型与目标 ID 过滤，回答「谁在何时改了什么」 */
    @GetMapping("/review-logs")
    public BaseResponse<Map<String, Object>> reviewLogs(@RequestParam(required = false) String targetType,
                                                        @RequestParam(required = false) Long targetId,
                                                        @RequestParam(defaultValue = "1") Long current,
                                                        @RequestParam(defaultValue = "20") Long size,
                                                        HttpServletRequest request) {
        require(request, "content:review");
        Page<com.picture_backend.model.entity.ReviewLog> page = reviewLogService.page(targetType, targetId, current, Math.min(size, 100));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", reviewLogService.decorate(page.getRecords()));
        data.put("total", page.getTotal());
        return ResultUtils.success(data);
    }

    /** 评论治理：列出评论（可按状态） */
    @GetMapping("/comments")
    public BaseResponse<Map<String, Object>> comments(@RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") Long current,
                                                      @RequestParam(defaultValue = "10") Long size,
                                                      HttpServletRequest request) {
        require(request, "content:review");
        Page<WorkComment> page = workCommentMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<WorkComment>()
                .eq(status != null && !status.isEmpty(), WorkComment::getStatus, status)
                .orderByDesc(WorkComment::getCreateTime));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("hiddenCount", workCommentMapper.selectCount(new LambdaQueryWrapper<WorkComment>().eq(WorkComment::getStatus, "HIDDEN")));
        return ResultUtils.success(data);
    }

    /** 评论隐藏 / 恢复 */
    @PostMapping("/comments/moderate")
    public BaseResponse<Boolean> moderateComment(@RequestBody CommentModerateRequest body, HttpServletRequest request) {
        User admin = require(request, "content:moderate");
        if (body == null || body.getId() == null || body.getAction() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean hide = "HIDE".equalsIgnoreCase(body.getAction());
        if (!hide && !"RESTORE".equalsIgnoreCase(body.getAction())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的动作");
        }
        WorkComment comment = workCommentMapper.selectById(body.getId());
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        }
        String from = comment.getStatus();
        workCommentMapper.update(null, new LambdaUpdateWrapper<WorkComment>()
                .eq(WorkComment::getId, comment.getId())
                .set(WorkComment::getStatus, hide ? "HIDDEN" : "VISIBLE"));
        reviewLogService.record("comment", comment.getId(), hide ? "HIDE" : "UNHIDE", from, hide ? "HIDDEN" : "VISIBLE",
                admin, body.getReason());
        if (hide && comment.getUserId() != null) {
            notificationService.push(comment.getUserId(), "SYSTEM", "你的评论已被隐藏",
                    "原因：" + (body.getReason() == null ? "内容不符合社区规范" : body.getReason()), null, "work", comment.getWorkId());
        }
        return ResultUtils.success(true);
    }

    @Data
    public static class CommentModerateRequest {
        private Long id;
        /** HIDE / RESTORE */
        private String action;
        private String reason;
    }
}
