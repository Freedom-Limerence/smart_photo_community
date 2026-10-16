package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.picture_backend.model.entity.FavoriteFolder;
import com.picture_backend.model.vo.FavoriteFolderVO;

import java.util.List;
import java.util.Map;

/** 收藏夹分组（P0 交互补齐） */
public interface FavoriteFolderService extends IService<FavoriteFolder> {

    Long createFolder(String name, String description, Long userId);

    void updateFolder(Long id, String name, String description, Long userId);

    /** 删除收藏夹：夹内作品回到「未分组」，不取消收藏 */
    void deleteFolder(Long id, Long userId);

    /** 收藏夹列表（含各自作品数；未分组以 id=null 由前端单独展示） */
    List<FavoriteFolderVO> listWithCount(Long userId);

    /** 把收藏记录移动到指定收藏夹（folderId 为空表示移出分组） */
    int moveFavorites(List<Long> workIds, Long folderId, Long userId);

    /** 批量取消收藏 */
    int cancelFavorites(List<Long> workIds, Long userId);

    /** 收藏夹内作品分页（folderId 为空表示未分组） */
    Map<String, Object> pageWorks(Long folderId, Long userId, long current, long size);
}
