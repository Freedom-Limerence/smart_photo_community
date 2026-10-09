package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.mapper.BrowseHistoryMapper;
import com.picture_backend.model.entity.BrowseHistory;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.BrowseHistoryService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 浏览历史服务实现
 */
@Service
public class BrowseHistoryServiceImpl extends ServiceImpl<BrowseHistoryMapper, BrowseHistory> implements BrowseHistoryService {

    @Async
    @Override
    public void recordBrowse(long pictureId, User loginUser) {
        // 同一用户同一图片只保留一条记录，刷新浏览时间
        BrowseHistory existHistory = this.lambdaQuery()
                .eq(BrowseHistory::getUserId, loginUser.getId())
                .eq(BrowseHistory::getPictureId, pictureId)
                .one();
        if (existHistory != null) {
            existHistory.setBrowseTime(new Date());
            this.updateById(existHistory);
        } else {
            BrowseHistory browseHistory = new BrowseHistory();
            browseHistory.setUserId(loginUser.getId());
            browseHistory.setPictureId(pictureId);
            browseHistory.setBrowseTime(new Date());
            this.save(browseHistory);
        }
    }

    @Override
    public Page<BrowseHistory> listMyHistory(long current, long size, User loginUser) {
        QueryWrapper<BrowseHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.orderByDesc("browseTime");
        return this.page(new Page<>(current, size), queryWrapper);
    }
}
