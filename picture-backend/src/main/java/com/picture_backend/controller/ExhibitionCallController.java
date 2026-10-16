package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.model.entity.ExhibitionCall;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.ExhibitionSubmissionVO;
import com.picture_backend.service.ExhibitionCallService;
import com.picture_backend.service.ExhibitionSubmissionService;
import com.picture_backend.service.RbacService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动·征稿（B2）：
 * <ul>
 *   <li>用户端：查看当前征稿、投稿自己的作品、查看我的投稿</li>
 *   <li>管理端（平台管理员或拥有 exhibition:manage 的策展人）：管理征稿启事、查看投稿、采纳/淘汰</li>
 * </ul>
 */
@RestController
@RequestMapping("/exhibition")
public class ExhibitionCallController {

    @Resource private ExhibitionCallService exhibitionCallService;
    @Resource private ExhibitionSubmissionService exhibitionSubmissionService;
    @Resource private UserService userService;
    @Resource private RbacService rbacService;

    /** 管理端权限：平台管理员 或 拥有 exhibition:manage 的角色（如策展人） */
    private User requireManage(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        if (userService.isAdmin(user) || rbacService.hasPermission(user, "exhibition:manage")) {
            return user;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限管理征稿");
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 用户端 ====================

    /** 当前可投稿的征稿（含我的投稿与剩余可投数量） */
    @GetMapping("/call/active")
    public BaseResponse<Map<String, Object>> activeCall(@RequestParam Long exhibitionId, HttpServletRequest request) {
        User viewer = userService.getLoginUserPermitNull(request);
        ExhibitionCall call = exhibitionCallService.getActiveCall(exhibitionId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("call", call);
        if (call == null || viewer == null) {
            data.put("submittedCount", 0);
            data.put("remaining", call == null ? 0 : (call.getMaxSubmissionsPerUser() == null ? 1 : call.getMaxSubmissionsPerUser()));
            data.put("mySubmissions", new ArrayList<>());
            return ResultUtils.success(data);
        }
        List<ExhibitionSubmissionVO> mine = exhibitionSubmissionService.listMine(call.getId(), viewer.getId());
        int max = call.getMaxSubmissionsPerUser() == null ? 1 : call.getMaxSubmissionsPerUser();
        data.put("submittedCount", mine.size());
        data.put("remaining", Math.max(0, max - mine.size()));
        data.put("mySubmissions", mine);
        return ResultUtils.success(data);
    }

    /** 投稿（支持一次勾选多件自己的作品） */
    @PostMapping("/call/submit")
    public BaseResponse<List<Long>> submit(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        Long callId = toLong(body.get("callId"));
        String caption = body.get("caption") == null ? null : String.valueOf(body.get("caption"));
        List<Long> workIds = new ArrayList<>();
        Object raw = body.get("workIds");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                Long id = toLong(item);
                if (id != null) {
                    workIds.add(id);
                }
            }
        }
        if (workIds.isEmpty()) {
            Long single = toLong(body.get("workId"));
            if (single != null) {
                workIds.add(single);
            }
        }
        return ResultUtils.success(exhibitionSubmissionService.submit(callId, workIds, caption, user));
    }

    /** 我在某征稿下的投稿 */
    @GetMapping("/call/my")
    public BaseResponse<List<ExhibitionSubmissionVO>> mySubmissions(@RequestParam Long callId,
                                                                    HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(exhibitionSubmissionService.listMine(callId, user.getId()));
    }

    /** 我的全部投稿（跨征稿） */
    @GetMapping("/call/my/all")
    public BaseResponse<List<ExhibitionSubmissionVO>> myAllSubmissions(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(exhibitionSubmissionService.listMineAll(user.getId()));
    }

    // ==================== 管理端：征稿启事 ====================

    @GetMapping("/call/list")
    public BaseResponse<List<ExhibitionCall>> listCalls(@RequestParam Long exhibitionId) {
        return ResultUtils.success(exhibitionCallService.listByExhibition(exhibitionId));
    }

    @PostMapping("/call/add")
    public BaseResponse<Long> addCall(@RequestBody ExhibitionCall call, HttpServletRequest request) {
        requireManage(request);
        return ResultUtils.success(exhibitionCallService.createCall(call));
    }

    @PostMapping("/call/update")
    public BaseResponse<Boolean> updateCall(@RequestBody ExhibitionCall call, HttpServletRequest request) {
        requireManage(request);
        exhibitionCallService.updateCall(call);
        return ResultUtils.success(true);
    }

    @PostMapping("/call/delete")
    public BaseResponse<Boolean> deleteCall(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireManage(request);
        exhibitionCallService.deleteCall(toLong(body.get("id")));
        return ResultUtils.success(true);
    }

    /** 开启 / 关闭征稿 */
    @PostMapping("/call/status")
    public BaseResponse<Boolean> changeStatus(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireManage(request);
        exhibitionCallService.changeStatus(toLong(body.get("id")), body.get("status") == null ? null : String.valueOf(body.get("status")));
        return ResultUtils.success(true);
    }

    // ==================== 管理端：投稿与评选 ====================

    @GetMapping("/submission/list")
    public BaseResponse<Map<String, Object>> listSubmissions(@RequestParam Long callId,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "1") long current,
                                                             @RequestParam(defaultValue = "10") long size,
                                                             HttpServletRequest request) {
        requireManage(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("page", exhibitionSubmissionService.pageByCall(callId, current, size, status));
        return ResultUtils.success(data);
    }

    /**
     * 采纳 / 淘汰投稿。采纳时会在事务内同时更新投稿状态并写入展品（exhibition_work）。
     */
    @PostMapping("/submission/review")
    public BaseResponse<Boolean> review(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User admin = requireManage(request);
        Long id = toLong(body.get("id"));
        Object acceptRaw = body.get("accept");
        boolean accept = acceptRaw instanceof Boolean flag ? flag : Boolean.parseBoolean(String.valueOf(acceptRaw));
        String caption = body.get("caption") == null ? null : String.valueOf(body.get("caption"));
        Long sortOrder = toLong(body.get("sortOrder"));
        exhibitionSubmissionService.review(id, accept, caption, sortOrder, admin);
        return ResultUtils.success(true);
    }
}
