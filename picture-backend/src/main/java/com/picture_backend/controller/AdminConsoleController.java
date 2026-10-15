package com.picture_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.annotation.LogRecord;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.model.dto.picture.PictureReviewRequest;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.enums.PictureReviewStatusEnum;
import com.picture_backend.service.ExhibitionService;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.PictureService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台控制台：数据看板、内容审核与治理。
 *
 * <p>与前台彻底分离：所有接口都要求管理员身份（userRole = admin），
 * 且只做治理动作，不提供上传/点赞/评论等前台行为。</p>
 */
@RestController
@RequestMapping("/admin/console")
public class AdminConsoleController {

    @Resource private UserService userService;
    @Resource private PictureService pictureService;
    @Resource private PhotoWorkService workService;
    @Resource private ExhibitionService exhibitionService;

    private User requireAdmin(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        if (!userService.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }
        return user;
    }

    private Date dayStart(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /** 看板总览：总量 + 今日新增 + 待处理事项 */
    @GetMapping("/overview")
    public BaseResponse<Map<String, Object>> overview(HttpServletRequest request) {
        requireAdmin(request);
        LocalDate today = LocalDate.now();
        Date start = dayStart(today);
        Date end = dayStart(today.plusDays(1));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", userService.count());
        data.put("pictures", pictureService.count());
        data.put("works", workService.countForAdmin(null, null, null, null));
        data.put("exhibitions", exhibitionService.count());
        data.put("newUsersToday", userService.count(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, start).lt(User::getCreateTime, end)));
        data.put("newPicturesToday", pictureService.count(new LambdaQueryWrapper<Picture>()
                .ge(Picture::getCreateTime, start).lt(Picture::getCreateTime, end)));
        data.put("newWorksToday", workService.countForAdmin(null, null, start, end));
        data.put("pendingPictures", pictureService.count(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getReviewStatus, PictureReviewStatusEnum.REVIEWING.getValue())));
        data.put("bannedUsers", userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getUserRole, "banned")));
        return ResultUtils.success(data);
    }

    /** 近 N 日增长趋势（默认 7 天），供 ECharts 折线使用 */
    @GetMapping("/trend")
    public BaseResponse<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") Integer days,
                                                         HttpServletRequest request) {
        requireAdmin(request);
        int span = Math.max(1, Math.min(days == null ? 7 : days, 90));
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = span - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Date start = dayStart(date);
            Date end = dayStart(date.plusDays(1));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", date.toString());
            row.put("users", userService.count(new LambdaQueryWrapper<User>()
                    .ge(User::getCreateTime, start).lt(User::getCreateTime, end)));
            row.put("pictures", pictureService.count(new LambdaQueryWrapper<Picture>()
                    .ge(Picture::getCreateTime, start).lt(Picture::getCreateTime, end)));
            row.put("works", workService.countForAdmin(null, null, start, end));
            list.add(row);
        }
        return ResultUtils.success(list);
    }

    /** 作品列表（管理视角，可按状态与标题筛选） */
    @GetMapping("/works")
    public BaseResponse<Page<PhotoWork>> works(@RequestParam(defaultValue = "1") Long current,
                                               @RequestParam(defaultValue = "10") Long size,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String keyword,
                                               HttpServletRequest request) {
        requireAdmin(request);
        return ResultUtils.success(workService.pageForAdmin(current, size, status, keyword));
    }

    /** 图片列表（管理视角，可按审核状态筛选） */
    @GetMapping("/pictures")
    public BaseResponse<Page<Picture>> pictures(@RequestParam(defaultValue = "1") Long current,
                                                @RequestParam(defaultValue = "10") Long size,
                                                @RequestParam(required = false) Integer reviewStatus,
                                                HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<Picture>()
                .eq(reviewStatus != null, Picture::getReviewStatus, reviewStatus)
                .orderByDesc(Picture::getCreateTime);
        return ResultUtils.success(pictureService.page(new Page<>(current, size), wrapper));
    }

    /**
     * 内容治理：图片批量通过/拒绝、作品批量封禁/恢复。
     * 每一步都会写入操作日志（@LogRecord）。
     */
    @PostMapping("/moderate")
    @LogRecord(action = "内容审核", module = "管理后台")
    public BaseResponse<Integer> moderate(@RequestBody ModerateRequest body, HttpServletRequest request) {
        User admin = requireAdmin(request);
        if (body == null || body.getIds() == null || body.getIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要处理的内容");
        }
        String action = body.getAction() == null ? "" : body.getAction().trim().toUpperCase();
        String type = body.getType() == null ? "" : body.getType().trim().toLowerCase();

        if ("picture".equals(type)) {
            Integer target;
            if ("PASS".equals(action)) {
                target = PictureReviewStatusEnum.PASS.getValue();
            } else if ("REJECT".equals(action)) {
                target = PictureReviewStatusEnum.REJECT.getValue();
            } else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的图片操作：" + action);
            }
            int affected = 0;
            for (Long id : body.getIds()) {
                if (id == null) continue;
                PictureReviewRequest reviewRequest = new PictureReviewRequest();
                reviewRequest.setId(id);
                reviewRequest.setReviewStatus(target);
                reviewRequest.setReviewMessage(StringUtils.hasText(body.getReason()) ? body.getReason() : null);
                pictureService.doPictureReview(reviewRequest, admin);
                affected++;
            }
            return ResultUtils.success(affected);
        }

        if ("work".equals(type)) {
            String target;
            if ("BAN".equals(action)) {
                target = "BANNED";
            } else if ("RESTORE".equals(action)) {
                target = "PUBLISHED";
            } else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的作品操作：" + action);
            }
            return ResultUtils.success(workService.updateStatusByIds(body.getIds(), target));
        }

        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的内容类型：" + type);
    }

    @Data
    public static class ModerateRequest {
        /** picture / work */
        private String type;
        /** PASS / REJECT / BAN / RESTORE */
        private String action;
        private List<Long> ids;
        private String reason;
    }
}
