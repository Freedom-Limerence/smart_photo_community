package com.picture_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.model.entity.Notification;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.NotificationService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 站内通知：列表 / 未读数 / 已读 */
@RestController
@RequestMapping("/notification")
public class NotificationController {
    /** 批量删除通知 */
    @PostMapping("/delete") public BaseResponse<Integer> delete(@RequestBody java.util.Map<String, Object> body, jakarta.servlet.http.HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        java.util.List<Long> ids = new java.util.ArrayList<>();
        Object raw = body.get("ids");
        if (raw instanceof java.util.List<?> list) {
            for (Object item : list) {
                if (item instanceof Number number) {
                    ids.add(number.longValue());
                } else if (item != null) {
                    try { ids.add(Long.parseLong(String.valueOf(item).trim())); } catch (NumberFormatException ignored) { }
                }
            }
        }
        return ResultUtils.success(notificationService.deleteNotifications(ids, user.getId()));
    }


    @Resource private NotificationService notificationService;
    @Resource private UserService userService;

    @GetMapping("/list")
    public BaseResponse<Page<Notification>> list(@RequestParam(defaultValue = "1") Long current,
                                                 @RequestParam(defaultValue = "20") Long size,
                                                 @RequestParam(required = false) Boolean onlyUnread,
                                                 HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(notificationService.page(user.getId(), current, Math.min(size, 50), onlyUnread));
    }

    /** 未读数（顶栏红点用，轮询调用） */
    @GetMapping("/unread/count")
    public BaseResponse<Map<String, Object>> unreadCount(HttpServletRequest request) {
        User user = userService.getLoginUserPermitNull(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", user == null ? 0L : notificationService.unreadCount(user.getId()));
        return ResultUtils.success(data);
    }

    @PostMapping("/read")
    public BaseResponse<Integer> read(@RequestBody ReadRequest body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        boolean all = body != null && Boolean.TRUE.equals(body.getAll());
        List<Long> ids = body == null ? null : body.getIds();
        return ResultUtils.success(notificationService.markRead(user.getId(), ids, all));
    }

    @Data
    public static class ReadRequest {
        private List<Long> ids;
        private Boolean all;
    }
}
