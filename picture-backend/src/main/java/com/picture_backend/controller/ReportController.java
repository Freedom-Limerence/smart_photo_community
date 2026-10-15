package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.ReportService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

/** 用户举报入口 */
@RestController
@RequestMapping("/report")
public class ReportController {

    @Resource private ReportService reportService;
    @Resource private UserService userService;

    @PostMapping("/submit")
    public BaseResponse<Long> submit(@RequestBody SubmitRequest body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        if (body == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = reportService.submit(user, body.getTargetType(), body.getTargetId(), body.getReason(), body.getDetail());
        return ResultUtils.success(id);
    }

    @Data
    public static class SubmitRequest {
        /** work / picture / comment / user */
        private String targetType;
        private Long targetId;
        private String reason;
        private String detail;
    }
}
