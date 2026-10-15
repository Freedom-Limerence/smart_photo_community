package com.picture_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.PageRequest;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.exception.ThrowUtils;
import com.picture_backend.model.entity.OperationLog;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.OperationLogService;
import com.picture_backend.service.RbacService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志接口：按 RBAC 权限点 log:view 校验（管理员与策展人都可查看审计记录）。
 */
@RestController
@RequestMapping("/log")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;
    @Resource
    private UserService userService;
    @Resource
    private RbacService rbacService;

    /**
     * 分页查询操作日志（需要 log:view 权限）
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<OperationLog>> listOperationLogByPage(@RequestBody PageRequest pageRequest,
                                                                   HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        if (!rbacService.hasPermission(user, "log:view")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少权限：log:view");
        }
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
