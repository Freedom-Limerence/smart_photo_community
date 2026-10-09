package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.picture_backend.model.entity.BrowseHistory;
import com.picture_backend.model.entity.User;

/**
 * 浏览历史服务
 */
public interface BrowseHistoryService extends IService<BrowseHistory> {

    /**
     * 记录浏览（同一用户同一图片短时间重复浏览只记一条最新时间）
     */
    void recordBrowse(long pictureId, User loginUser);

    /**
     * 获取我的浏览历史分页（按浏览时间倒序）
     */
    Page<BrowseHistory> listMyHistory(long current, long size, User loginUser);
}
