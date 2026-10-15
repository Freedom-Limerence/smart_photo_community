package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.AnnouncementMapper;
import com.picture_backend.model.entity.Announcement;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.AnnouncementService;
import com.picture_backend.service.NotificationService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Resource private AnnouncementMapper announcementMapper;
    @Resource private NotificationService notificationService;
    @Resource private UserService userService;

    @Override
    public List<Announcement> latest(int limit) {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, "PUBLISHED")
                .orderByDesc(Announcement::getPublishTime)
                .last("limit " + Math.max(1, Math.min(limit, 20))));
    }

    @Override
    public Page<Announcement> page(String status, long current, long size) {
        return announcementMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Announcement>()
                .eq(StringUtils.hasText(status), Announcement::getStatus, status)
                .orderByDesc(Announcement::getCreateTime));
    }

    @Override
    public Long save(User publisher, Announcement announcement) {
        if (announcement == null || !StringUtils.hasText(announcement.getTitle()) || !StringUtils.hasText(announcement.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题与内容不能为空");
        }
        announcement.setPublisherId(publisher == null ? null : publisher.getId());
        if (!StringUtils.hasText(announcement.getLevel())) {
            announcement.setLevel("INFO");
        }
        if (announcement.getId() == null) {
            if (!StringUtils.hasText(announcement.getStatus())) {
                announcement.setStatus("DRAFT");
            }
            announcementMapper.insert(announcement);
        } else {
            announcementMapper.updateById(announcement);
        }
        return announcement.getId();
    }

    @Override
    public boolean changeStatus(User publisher, Long id, String status, boolean broadcast) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "公告不存在");
        }
        List<String> allowed = List.of("DRAFT", "PUBLISHED", "OFFLINE");
        if (!allowed.contains(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的状态");
        }
        announcement.setStatus(status);
        if ("PUBLISHED".equals(status)) {
            announcement.setPublishTime(new Date());
        }
        announcementMapper.updateById(announcement);
        // 发布时推送给全体用户（站内通知）
        if (broadcast && "PUBLISHED".equals(status)) {
            List<User> users = userService.list(new LambdaQueryWrapper<User>().eq(User::getIsDelete, 0));
            for (User user : users) {
                notificationService.push(user.getId(), "SYSTEM", "平台公告：" + announcement.getTitle(),
                        announcement.getContent().length() > 120 ? announcement.getContent().substring(0, 120) + "..." : announcement.getContent(),
                        null, "announcement", announcement.getId());
            }
            log.info("[Announcement] 公告 {} 已发布并推送给 {} 位用户", announcement.getId(), users.size());
        }
        return true;
    }
}
