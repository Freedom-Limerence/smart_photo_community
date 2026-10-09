package com.picture_backend.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.annotation.AuthCheck;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.PageRequest;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.constant.UserConstant;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.exception.ThrowUtils;
import com.picture_backend.model.entity.OperationLog;
import com.picture_backend.service.OperationLogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 操作日志接口（仅管理员）
 */
@RestController
@RequestMapping("/log")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;

    /**
     * 分页查询操作日志（仅管理员）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<OperationLog>> listOperationLogByPage(@RequestBody PageRequest pageRequest) {
        ThrowUtils.throwIf(pageRequest == null, ErrorCode.PARAMS_ERROR);
        long current = pageRequest.getCurrent();
        long size = pageRequest.getPageSize();
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR, "每页最多 50 条");
        QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("createTime");
        Page<OperationLog> operationLogPage = operationLogService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(operationLogPage);
    }
}
