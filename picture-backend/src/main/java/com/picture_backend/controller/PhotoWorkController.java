package com.picture_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.model.dto.work.*;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.entity.WorkComment;
import com.picture_backend.service.NotificationService;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.UserService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/work")
public class PhotoWorkController {
    @Resource private PhotoWorkService workService;
    @Resource private UserService userService;
    @Resource private NotificationService notificationService;
    @PostMapping("/save") public BaseResponse<Long> save(@RequestBody PhotoWorkRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.saveWork(body, user.getId())); }
    @PostMapping("/add") public BaseResponse<Long> add(@RequestBody PhotoWorkRequest body, HttpServletRequest request) { return save(body, request); }
    @PostMapping("/update") public BaseResponse<Long> update(@RequestBody PhotoWorkRequest body, HttpServletRequest request) { return save(body, request); }
    @PostMapping("/publish") public BaseResponse<Boolean> publish(@RequestParam Long workId, @RequestParam(defaultValue = "true") Boolean publish, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.publish(workId, user.getId(), publish)); }
    @PostMapping("/offline") public BaseResponse<Boolean> offline(@RequestParam Long workId, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.publish(workId, user.getId(), false)); }
    @GetMapping("/get") public BaseResponse<PhotoWork> get(@RequestParam Long workId, HttpServletRequest request) { User user = userService.getLoginUserPermitNull(request); return ResultUtils.success(workService.getVisible(workId, user == null ? null : user.getId())); }
    @GetMapping("/get/vo") public BaseResponse<PhotoWork> getVo(@RequestParam("id") Long workId, HttpServletRequest request) { return get(workId, request); }
    @GetMapping("/page") public BaseResponse<Page<PhotoWork>> page(@RequestParam(defaultValue = "1") Long current, @RequestParam(defaultValue = "20") Long size, @RequestParam(required = false) Long authorId, @RequestParam(defaultValue = "latest") String sort, @RequestParam(defaultValue = "all") String feed, HttpServletRequest request) { User viewer = userService.getLoginUserPermitNull(request); Long viewerId = viewer == null ? null : viewer.getId(); if (authorId != null) { return ResultUtils.success(workService.pageByAuthor(authorId, current, size, viewerId)); } if ("following".equalsIgnoreCase(feed) && viewerId != null) { return ResultUtils.success(workService.pageFollowing(current, size, viewerId)); } return ResultUtils.success(workService.page(current, size, null, sort)); }
    @PostMapping("/like") public BaseResponse<Boolean> like(@RequestBody WorkInteractionRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); boolean changed = workService.like(body.getWorkId(), user.getId(), true); if (changed) { notificationService.notifyWork(body.getWorkId(), user.getId(), "LIKE"); } return ResultUtils.success(true); }
    @PostMapping("/unlike") public BaseResponse<Boolean> unlike(@RequestBody WorkInteractionRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); workService.like(body.getWorkId(), user.getId(), false); return ResultUtils.success(true); }
    @PostMapping("/favorite") public BaseResponse<Boolean> favorite(@RequestBody WorkInteractionRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); boolean changed = workService.favorite(body.getWorkId(), user.getId(), true); if (changed) { notificationService.notifyWork(body.getWorkId(), user.getId(), "FAVORITE"); } return ResultUtils.success(true); }
    @PostMapping("/unfavorite") public BaseResponse<Boolean> unfavorite(@RequestBody WorkInteractionRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); workService.favorite(body.getWorkId(), user.getId(), false); return ResultUtils.success(true); }
    @PostMapping("/comment") public BaseResponse<WorkComment> comment(@RequestBody WorkCommentRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); WorkComment comment = workService.addComment(body.getWorkId(), user.getId(), body.getParentId(), body.getContent()); notificationService.notifyWork(body.getWorkId(), user.getId(), "COMMENT"); return ResultUtils.success(comment); }
    @GetMapping("/comments") public BaseResponse<List<WorkComment>> comments(@RequestParam Long workId, HttpServletRequest request) { User viewer = userService.getLoginUserPermitNull(request); return ResultUtils.success(workService.listComments(workId, viewer == null ? null : viewer.getId())); }
    @PostMapping("/follow") public BaseResponse<Boolean> follow(@RequestBody FollowRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); boolean changed = workService.follow(body.getUserId(), user.getId(), true); if (changed) { notificationService.notifyFollow(body.getUserId(), user.getId()); } return ResultUtils.success(true); }
    @PostMapping("/unfollow") public BaseResponse<Boolean> unfollow(@RequestBody FollowRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); workService.follow(body.getUserId(), user.getId(), false); return ResultUtils.success(true); }

    /** 相关推荐：同摄影师优先 + 最新作品补足 */
    @GetMapping("/related") public BaseResponse<List<PhotoWork>> related(@RequestParam Long id, @RequestParam(defaultValue = "6") Integer limit) { return ResultUtils.success(workService.related(id, limit)); }

    /** 评论点赞 / 取消 */
    @PostMapping("/comment/like") public BaseResponse<Boolean> likeComment(@RequestBody WorkInteractionRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.likeComment(body.getWorkId(), user.getId(), true)); }
    @PostMapping("/comment/unlike") public BaseResponse<Boolean> unlikeComment(@RequestBody WorkInteractionRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.likeComment(body.getWorkId(), user.getId(), false)); }

    /** 我的收藏（按收藏时间倒序，供「我的收藏」页使用） */
    @GetMapping("/favorites") public BaseResponse<List<PhotoWork>> favorites(@RequestParam(defaultValue = "60") Integer limit, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.listFavorites(user.getId(), limit)); }

    /** 我的喜欢 / 点赞（按点赞时间倒序） */
    @GetMapping("/likes") public BaseResponse<List<PhotoWork>> likes(@RequestParam(defaultValue = "60") Integer limit, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(workService.listLikes(user.getId(), limit)); }

    /** 作者数据表现：我的作品汇总 + 表现最好的作品（浏览/点赞/收藏/评论） */
    @GetMapping("/stats/mine")
    public BaseResponse<Map<String, Object>> myStats(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        List<PhotoWork> works = workService.pageByAuthor(user.getId(), 1, 200, user.getId()).getRecords();
        long views = 0L;
        long likes = 0L;
        long favorites = 0L;
        long comments = 0L;
        for (PhotoWork work : works) {
            views += work.getViewCount() == null ? 0L : work.getViewCount();
            likes += work.getLikeCount() == null ? 0L : work.getLikeCount();
            favorites += work.getFavoriteCount() == null ? 0L : work.getFavoriteCount();
            comments += work.getCommentCount() == null ? 0L : work.getCommentCount();
        }
        List<PhotoWork> top = new ArrayList<>(works);
        top.sort(Comparator.comparingLong((PhotoWork w) -> (w.getViewCount() == null ? 0L : w.getViewCount())
                + (w.getLikeCount() == null ? 0L : w.getLikeCount()) * 3
                + (w.getFavoriteCount() == null ? 0L : w.getFavoriteCount()) * 5).reversed());
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (PhotoWork work : top.subList(0, Math.min(5, top.size()))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", work.getId());
            row.put("title", work.getTitle());
            row.put("coverUrl", work.getCoverUrl());
            row.put("viewCount", work.getViewCount() == null ? 0L : work.getViewCount());
            row.put("likeCount", work.getLikeCount() == null ? 0L : work.getLikeCount());
            row.put("favoriteCount", work.getFavoriteCount() == null ? 0L : work.getFavoriteCount());
            row.put("commentCount", work.getCommentCount() == null ? 0L : work.getCommentCount());
            ranking.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("workCount", works.size());
        data.put("viewCount", views);
        data.put("likeCount", likes);
        data.put("favoriteCount", favorites);
        data.put("commentCount", comments);
        data.put("ranking", ranking);
        return ResultUtils.success(data);
    }
}
