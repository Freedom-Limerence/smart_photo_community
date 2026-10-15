package com.picture_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.model.entity.Appeal;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.AppealService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 审核申诉（作者侧） */
@RestController
@RequestMapping("/appeal")
public class AppealController {

    @Resource private AppealService appealService;
    @Resource private UserService userService;

    @PostMapping("/submit")
    public BaseResponse<Long> submit(@RequestBody SubmitRequest body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        if (body == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(appealService.submit(user, body.getPictureId(), body.getReason()));
    }

    /** 我的申诉（含图片名称、状态与处理结果） */
    @GetMapping("/mine")
    public BaseResponse<Map<String, Object>> mine(@RequestParam(defaultValue = "1") Long current,
                                                  @RequestParam(defaultValue = "10") Long size,
                                                  HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        Page<Appeal> page = appealService.pageMine(user.getId(), current, Math.min(size, 50));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", appealService.decorate(page.getRecords()));
        data.put("total", page.getTotal());
        return ResultUtils.success(data);
    }

    @Data
    public static class SubmitRequest {
        private Long pictureId;
        private String reason;
    }
}
