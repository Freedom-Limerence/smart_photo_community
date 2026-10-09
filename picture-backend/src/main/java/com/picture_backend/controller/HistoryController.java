package com.picture_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.exception.ThrowUtils;
import com.picture_backend.model.entity.BrowseHistory;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.BrowseHistoryService;
import com.picture_backend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 浏览历史接口
 */
@RestController
@RequestMapping("/history")
public class HistoryController {

    @Resource
    private BrowseHistoryService browseHistoryService;

    @Resource
    private UserService userService;

    /**
     * 记录浏览（图片详情页触发）
     */
    @PostMapping("/record")
    public BaseResponse<Boolean> recordBrowse(long pictureId, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        browseHistoryService.recordBrowse(pictureId, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取我的浏览历史（分页）
     */
    @GetMapping("/list/my")
    public BaseResponse<Page<BrowseHistory>> listMyHistory(long current, long size, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR, "每页最多 50 条");
        return ResultUtils.success(browseHistoryService.listMyHistory(current, size, loginUser));
    }
}
