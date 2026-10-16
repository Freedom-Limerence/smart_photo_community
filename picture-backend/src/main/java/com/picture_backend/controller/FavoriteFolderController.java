package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.FavoriteFolderVO;
import com.picture_backend.service.FavoriteFolderService;
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

/** 收藏夹分组与收藏管理（P0 交互补齐） */
@RestController
@RequestMapping("/favorite")
public class FavoriteFolderController {

    @Resource private FavoriteFolderService favoriteFolderService;
    @Resource private UserService userService;

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

    private List<Long> toIds(Object raw) {
        List<Long> ids = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                Long id = toLong(item);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /** 我的收藏夹列表（含各自作品数）与未分组数量 */
    @GetMapping("/folders")
    public BaseResponse<Map<String, Object>> folders(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        List<FavoriteFolderVO> folders = favoriteFolderService.listWithCount(user.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("folders", folders);
        data.put("totalFolders", folders.size());
        Long ungrouped = 0L;
        for (FavoriteFolderVO folder : folders) {
            ungrouped += folder.getWorkCount() == null ? 0L : folder.getWorkCount();
        }
        data.put("folderedCount", ungrouped);
        return ResultUtils.success(data);
    }

    @PostMapping("/folder/add")
    public BaseResponse<Long> addFolder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        String name = body.get("name") == null ? null : String.valueOf(body.get("name"));
        String description = body.get("description") == null ? null : String.valueOf(body.get("description"));
        return ResultUtils.success(favoriteFolderService.createFolder(name, description, user.getId()));
    }

    @PostMapping("/folder/update")
    public BaseResponse<Boolean> updateFolder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        String name = body.get("name") == null ? null : String.valueOf(body.get("name"));
        String description = body.get("description") == null ? null : String.valueOf(body.get("description"));
        favoriteFolderService.updateFolder(toLong(body.get("id")), name, description, user.getId());
        return ResultUtils.success(true);
    }

    @PostMapping("/folder/delete")
    public BaseResponse<Boolean> deleteFolder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        favoriteFolderService.deleteFolder(toLong(body.get("id")), user.getId());
        return ResultUtils.success(true);
    }

    /** 把收藏的作品移动到指定收藏夹（folderId 传空表示移出分组） */
    @PostMapping("/move")
    public BaseResponse<Integer> move(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(favoriteFolderService.moveFavorites(
                toIds(body.get("workIds")), toLong(body.get("folderId")), user.getId()));
    }

    /** 批量取消收藏 */
    @PostMapping("/cancel")
    public BaseResponse<Integer> cancel(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(favoriteFolderService.cancelFavorites(
                toIds(body.get("workIds")), user.getId()));
    }

    /** 收藏夹内作品分页（folderId 传空表示未分组） */
    @GetMapping("/works")
    public BaseResponse<Map<String, Object>> works(@RequestParam(required = false) Long folderId,
                                                   @RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "24") long size,
                                                   HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(favoriteFolderService.pageWorks(folderId, user.getId(), current, size));
    }
}
