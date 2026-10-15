package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.model.entity.Notification;

public interface NotificationService {
    /** 创建一条通知（自己触发自己的动作不会通知） */
    void push(Long userId, String type, String title, String content, Long senderId, String targetType, Long targetId);

    Page<Notification> page(Long userId, long current, long size, Boolean onlyUnread);

    long unreadCount(Long userId);

    /** 标记已读：ids 为空且 all=true 时全部已读 */
    int markRead(Long userId, java.util.List<Long> ids, boolean all);

    /** 作品被点赞/收藏/评论、被关注 的便捷推送 */
    void notifyWork(Long workId, Long senderId, String type);

    void notifyFollow(Long followingId, Long followerId);

    void notifyReportResult(Long reporterId, Long reportId, boolean resolved, String result);
}
