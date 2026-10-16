package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.mapper.NotificationMapper;
import com.picture_backend.model.entity.Notification;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.NotificationService;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
    @Override
    public int deleteNotifications(java.util.List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty() || userId == null) {
            return 0;
        }
        return notificationMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.picture_backend.model.entity.Notification>()
                .eq(com.picture_backend.model.entity.Notification::getUserId, userId)
                .in(com.picture_backend.model.entity.Notification::getId, ids));
    }


    @Resource private NotificationMapper notificationMapper;
    @Resource private PhotoWorkService photoWorkService;
    @Resource private UserService userService;

    @Override
    public void push(Long userId, String type, String title, String content, Long senderId, String targetType, Long targetId) {
        if (userId == null || senderId != null && senderId.equals(userId)) {
            return;
        }
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setSenderId(senderId);
            notification.setTargetType(targetType);
            notification.setTargetId(targetId);
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        } catch (Exception e) {
            // 通知失败不能影响主流程
            log.warn("[Notify] 写入通知失败: {}", e.getMessage());
        }
    }

    @Override
    public Page<Notification> page(Long userId, long current, long size, Boolean onlyUnread) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Boolean.TRUE.equals(onlyUnread), Notification::getIsRead, 0)
                .orderByDesc(Notification::getCreateTime);
        Page<Notification> result = notificationMapper.selectPage(new Page<>(current, size), wrapper);
        // 补充触发者昵称，便于前端展示
        result.getRecords().forEach(n -> {
            if (n.getSenderId() != null) {
                User sender = userService.getById(n.getSenderId());
                if (sender != null) {
                    n.setContent((n.getContent() == null ? "" : n.getContent()));
                    n.setTargetType(n.getTargetType());
                }
            }
        });
        return result;
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId).eq(Notification::getIsRead, 0));
    }

    @Override
    public int markRead(Long userId, List<Long> ids, boolean all) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        if (!all) {
            if (ids == null || ids.isEmpty()) {
                return 0;
            }
            wrapper.in(Notification::getId, ids);
        }
        return notificationMapper.update(null, wrapper);
    }

    @Override
    public void notifyWork(Long workId, Long senderId, String type) {
        PhotoWork work = photoWorkService.getById(workId);
        if (work == null || work.getAuthorId() == null) {
            return;
        }
        User sender = userService.getById(senderId);
        String who = sender == null || !StringUtils.hasText(sender.getUserName()) ? "有人" : sender.getUserName();
        String label = "LIKE".equals(type) ? "点赞了" : "FAVORITE".equals(type) ? "收藏了" : "评论了";
        push(work.getAuthorId(), type, who + " " + label + "你的作品",
                "《" + (work.getTitle() == null ? "未命名作品" : work.getTitle()) + "》", senderId, "work", workId);
    }

    @Override
    public void notifyFollow(Long followingId, Long followerId) {
        User follower = userService.getById(followerId);
        String who = follower == null || !StringUtils.hasText(follower.getUserName()) ? "有人" : follower.getUserName();
        push(followingId, "FOLLOW", who + " 关注了你", "去看看 TA 的主页", followerId, "user", followerId);
    }

    @Override
    public void notifyReportResult(Long reporterId, Long reportId, boolean resolved, String result) {
        push(reporterId, "SYSTEM", "你的举报已处理",
                (resolved ? "我们已对被举报内容采取措施。" : "经核实该内容未违规。") + (StringUtils.hasText(result) ? "（" + result + "）" : ""),
                null, "report", reportId);
    }
}
