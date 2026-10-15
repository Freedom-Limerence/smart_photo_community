package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.model.entity.Announcement;
import com.picture_backend.model.entity.User;

import java.util.List;

public interface AnnouncementService {
    List<Announcement> latest(int limit);

    Page<Announcement> page(String status, long current, long size);

    Long save(User publisher, Announcement announcement);

    boolean changeStatus(User publisher, Long id, String status, boolean broadcast);
}
