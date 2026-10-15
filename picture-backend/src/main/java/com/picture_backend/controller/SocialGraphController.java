package com.picture_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.exception.ThrowUtils;
import com.picture_backend.mapper.UserFollowMapper;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.entity.UserFollow;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 摄影师公开关注关系；只返回展示所需的公开资料。 */
@RestController
@RequestMapping("/social")
public class SocialGraphController {
    @Resource private UserFollowMapper followMapper;
    @Resource private UserService userService;

    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> stats(@RequestParam Long userId, HttpServletRequest request) {
        ThrowUtils.throwIf(userId == null || userService.getById(userId) == null, ErrorCode.NOT_FOUND_ERROR);
        User viewer = userService.getLoginUserPermitNull(request);
        long followers = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowingId, userId));
        long following = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));
        boolean followed = viewer != null && followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, viewer.getId()).eq(UserFollow::getFollowingId, userId)) > 0;
        return ResultUtils.success(Map.of("followers", followers, "following", following, "followed", followed));
    }

    @GetMapping("/followers")
    public BaseResponse<Page<FollowUserVO>> followers(@RequestParam Long userId,
            @RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size) {
        return ResultUtils.success(list(userId, current, size, true));
    }

    @GetMapping("/following")
    public BaseResponse<Page<FollowUserVO>> following(@RequestParam Long userId,
            @RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size) {
        return ResultUtils.success(list(userId, current, size, false));
    }

    private Page<FollowUserVO> list(Long userId, long current, long size, boolean followers) {
        ThrowUtils.throwIf(userId == null || userService.getById(userId) == null || current < 1 || size < 1 || size > 50,
                ErrorCode.PARAMS_ERROR);
        LambdaQueryWrapper<UserFollow> query = new LambdaQueryWrapper<UserFollow>().orderByDesc(UserFollow::getCreateTime);
        if (followers) query.eq(UserFollow::getFollowingId, userId);
        else query.eq(UserFollow::getFollowerId, userId);
        Page<UserFollow> relations = followMapper.selectPage(new Page<>(current, size), query);
        List<Long> ids = relations.getRecords().stream().map(followers ? UserFollow::getFollowerId : UserFollow::getFollowingId).toList();
        Map<Long, User> users = ids.isEmpty() ? Map.of() : userService.listByIds(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Page<FollowUserVO> result = new Page<>(current, size, relations.getTotal());
        result.setRecords(ids.stream().map(users::get).filter(Objects::nonNull).map(user -> {
            FollowUserVO vo = new FollowUserVO();
            vo.setId(user.getId()); vo.setUserName(user.getUserName()); vo.setUserAvatar(user.getUserAvatar()); vo.setUserProfile(user.getUserProfile());
            return vo;
        }).toList());
        return result;
    }

    @Data
    public static class FollowUserVO {
        private Long id;
        private String userName;
        private String userAvatar;
        private String userProfile;
    }
}
