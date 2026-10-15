package com.picture_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.mapper.UserFollowMapper;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.entity.UserFollow;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.PictureService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户成就：等级与徽章由真实数据推导（作品数 / 获赞 / 被收藏 / 粉丝 / 图库），不依赖新表。
 */
@RestController
@RequestMapping("/user/achievement")
public class AchievementController {

    private static final int[] LEVEL_THRESHOLDS = {0, 60, 200, 500, 1200, 3000};
    private static final String[] LEVEL_NAMES = {"初学取景", "光影学徒", "熟练摄影人", "资深创作者", "影像策展人", "光影大师"};

    @Resource private UserService userService;
    @Resource private PhotoWorkService photoWorkService;
    @Resource private PictureService pictureService;
    @Resource private UserFollowMapper userFollowMapper;

    @GetMapping
    public BaseResponse<Map<String, Object>> achievement(@RequestParam(required = false) Long userId,
                                                         HttpServletRequest request) {
        User viewer = userService.getLoginUserPermitNull(request);
        Long targetId = userId != null ? userId : (viewer == null ? null : viewer.getId());
        if (targetId == null) {
            return ResultUtils.success(new LinkedHashMap<>());
        }
        User user = userService.getById(targetId);
        List<PhotoWork> works = photoWorkService.pageByAuthor(targetId, 1, 200, targetId).getRecords();
        long likes = 0L;
        long favorites = 0L;
        long views = 0L;
        for (PhotoWork work : works) {
            likes += work.getLikeCount() == null ? 0L : work.getLikeCount();
            favorites += work.getFavoriteCount() == null ? 0L : work.getFavoriteCount();
            views += work.getViewCount() == null ? 0L : work.getViewCount();
        }
        long pictures = pictureService.count(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getUserId, targetId).eq(Picture::getIsDelete, 0));
        long followers = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowingId, targetId));
        long following = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, targetId));

        long score = works.size() * 10L + likes * 2L + favorites * 3L + followers * 5L + pictures;
        int level = 1;
        for (int i = 0; i < LEVEL_THRESHOLDS.length; i++) {
            if (score >= LEVEL_THRESHOLDS[i]) {
                level = i + 1;
            }
        }
        long currentFloor = LEVEL_THRESHOLDS[level - 1];
        long next = level >= LEVEL_THRESHOLDS.length ? currentFloor : LEVEL_THRESHOLDS[level];
        int progress = next <= currentFloor ? 100
                : (int) Math.min(100, Math.round((score - currentFloor) * 100.0 / (next - currentFloor)));

        List<Map<String, Object>> badges = new ArrayList<>();
        badges.add(badge("首发作品", "发布第一件作品", works.size() >= 1));
        badges.add(badge("十件作品", "发布 10 件作品", works.size() >= 10));
        badges.add(badge("百人点赞", "累计获得 100 个赞", likes >= 100));
        badges.add(badge("人气作者", "累计获得 500 个赞", likes >= 500));
        badges.add(badge("收藏达人", "作品被收藏 50 次", favorites >= 50));
        badges.add(badge("图库丰富", "图库超过 50 张图片", pictures >= 50));
        badges.add(badge("小有名气", "粉丝超过 20 人", followers >= 20));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", targetId);
        data.put("userName", user == null ? null : user.getUserName());
        data.put("score", score);
        data.put("level", level);
        data.put("levelName", LEVEL_NAMES[level - 1]);
        data.put("progress", progress);
        data.put("nextLevelScore", next);
        data.put("workCount", works.size());
        data.put("pictureCount", pictures);
        data.put("likeCount", likes);
        data.put("favoriteCount", favorites);
        data.put("viewCount", views);
        data.put("followerCount", followers);
        data.put("followingCount", following);
        data.put("badges", badges);
        return ResultUtils.success(data);
    }

    private Map<String, Object> badge(String name, String description, boolean owned) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("description", description);
        item.put("owned", owned);
        return item;
    }
}
