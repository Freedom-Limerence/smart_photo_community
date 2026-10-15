package com.picture_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.model.dto.work.FollowRequest;
import com.picture_backend.model.dto.work.PhotoWorkRequest;
import com.picture_backend.model.dto.work.WorkCommentRequest;
import com.picture_backend.model.dto.work.WorkInteractionRequest;
import com.picture_backend.model.dto.work.*;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.entity.WorkComment;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.UserService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/work")
public class PhotoWorkController {
    @Resource private PhotoWorkService workService;
    @Resource private UserService userService;
    @PostMapping("/save") public BaseResponse<Long> save(@RequestBody PhotoWorkRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.saveWork(body, user.getId())); }
    @PostMapping("/add") public BaseResponse<Long> add(@RequestBody PhotoWorkRequest body, HttpServletRequest request) { return save(body, request); }
    @PostMapping("/update") public BaseResponse<Long> update(@RequestBody PhotoWorkRequest body, HttpServletRequest request) { return save(body, request); }
    @PostMapping("/publish") public BaseResponse<Boolean> publish(@RequestParam Long workId, @RequestParam(defaultValue = "true") Boolean publish, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.publish(workId, user.getId(), publish)); }
    @PostMapping("/offline") public BaseResponse<Boolean> offline(@RequestParam Long workId, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.publish(workId, user.getId(), false)); }
    @GetMapping("/get") public BaseResponse<PhotoWork> get(@RequestParam Long workId, HttpServletRequest request) { User user = userService.getLoginUserPermitNull(request); return ResultUtils.success(workService.getVisible(workId, user == null ? null : user.getId())); }
    @GetMapping("/get/vo") public BaseResponse<PhotoWork> getVo(@RequestParam("id") Long workId, HttpServletRequest request) { return get(workId, request); }
    @GetMapping("/page") public BaseResponse<Page<PhotoWork>> page(@RequestParam(defaultValue = "1") Long current, @RequestParam(defaultValue = "20") Long size, @RequestParam(required = false) Long authorId, @RequestParam(defaultValue = "latest") String sort, HttpServletRequest request) { User viewer = userService.getLoginUserPermitNull(request); Long viewerId = viewer == null ? null : viewer.getId(); if (authorId != null) { return ResultUtils.success(workService.pageByAuthor(authorId, current, size, viewerId)); } return ResultUtils.success(workService.page(current, size, null, sort)); }
    @PostMapping("/like") public BaseResponse<Boolean> like(@RequestBody WorkInteractionRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.like(body.getWorkId(), user.getId(), true)); }
    @PostMapping("/unlike") public BaseResponse<Boolean> unlike(@RequestBody WorkInteractionRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.like(body.getWorkId(), user.getId(), false)); }
    @PostMapping("/favorite") public BaseResponse<Boolean> favorite(@RequestBody WorkInteractionRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.favorite(body.getWorkId(), user.getId(), true)); }
    @PostMapping("/unfavorite") public BaseResponse<Boolean> unfavorite(@RequestBody WorkInteractionRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.favorite(body.getWorkId(), user.getId(), false)); }
    @PostMapping("/comment") public BaseResponse<WorkComment> comment(@RequestBody WorkCommentRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.addComment(body.getWorkId(), user.getId(), body.getParentId(), body.getContent())); }
    @GetMapping("/comments") public BaseResponse<List<WorkComment>> comments(@RequestParam Long workId, HttpServletRequest request) { User viewer = userService.getLoginUserPermitNull(request); return ResultUtils.success(workService.listComments(workId, viewer == null ? null : viewer.getId())); }
    @PostMapping("/follow") public BaseResponse<Boolean> follow(@RequestBody FollowRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.follow(body.getUserId(), user.getId(), true)); }
    @PostMapping("/unfollow") public BaseResponse<Boolean> unfollow(@RequestBody FollowRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.follow(body.getUserId(), user.getId(), false)); }

    /** 我的收藏（按收藏时间倒序，供「我的收藏」页使用） */
    @GetMapping("/favorites") public BaseResponse<List<PhotoWork>> favorites(@RequestParam(defaultValue = "60") Integer limit, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.listFavorites(user.getId(), limit)); }

    /** 我的喜欢 / 点赞（按点赞时间倒序） */
    @GetMapping("/likes") public BaseResponse<List<PhotoWork>> likes(@RequestParam(defaultValue = "60") Integer limit, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.listLikes(user.getId(), limit)); }
}
