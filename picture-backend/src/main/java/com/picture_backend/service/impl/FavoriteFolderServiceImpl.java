package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.FavoriteFolderMapper;
import com.picture_backend.mapper.WorkFavoriteMapper;
import com.picture_backend.model.entity.FavoriteFolder;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.WorkFavorite;
import com.picture_backend.model.vo.FavoriteFolderVO;
import com.picture_backend.service.FavoriteFolderService;
import com.picture_backend.service.PhotoWorkService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FavoriteFolderServiceImpl extends ServiceImpl<FavoriteFolderMapper, FavoriteFolder>
        implements FavoriteFolderService {

    @Resource private WorkFavoriteMapper workFavoriteMapper;
    @Resource private PhotoWorkService photoWorkService;

    @Override
    public Long createFolder(String name, String description, Long userId) {
        if (userId == null || !StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收藏夹名称不能为空");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收藏夹名称不能超过 64 个字");
        }
        Long exists = baseMapper.selectCount(new LambdaQueryWrapper<FavoriteFolder>()
                .eq(FavoriteFolder::getUserId, userId)
                .eq(FavoriteFolder::getName, trimmed));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已存在同名收藏夹");
        }
        FavoriteFolder folder = new FavoriteFolder();
        folder.setUserId(userId);
        folder.setName(trimmed);
        folder.setDescription(description);
        folder.setSortOrder(0);
        save(folder);
        return folder.getId();
    }

    @Override
    public void updateFolder(Long id, String name, String description, Long userId) {
        FavoriteFolder folder = requireOwned(id, userId);
        FavoriteFolder patch = new FavoriteFolder();
        patch.setId(folder.getId());
        if (StringUtils.hasText(name)) {
            String trimmed = name.trim();
            Long exists = baseMapper.selectCount(new LambdaQueryWrapper<FavoriteFolder>()
                    .eq(FavoriteFolder::getUserId, userId)
                    .eq(FavoriteFolder::getName, trimmed)
                    .ne(FavoriteFolder::getId, id));
            if (exists != null && exists > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已存在同名收藏夹");
            }
            patch.setName(trimmed);
        }
        patch.setDescription(description);
        updateById(patch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long id, Long userId) {
        FavoriteFolder folder = requireOwned(id, userId);
        // 夹内作品回到「未分组」，收藏关系保留
        workFavoriteMapper.update(null, new LambdaUpdateWrapper<WorkFavorite>()
                .eq(WorkFavorite::getUserId, userId)
                .eq(WorkFavorite::getFolderId, folder.getId())
                .set(WorkFavorite::getFolderId, null));
        removeById(folder.getId());
    }

    private FavoriteFolder requireOwned(Long id, Long userId) {
        if (id == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        FavoriteFolder folder = getById(id);
        if (folder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "收藏夹不存在");
        }
        if (!userId.equals(folder.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作他人的收藏夹");
        }
        return folder;
    }

    @Override
    public List<FavoriteFolderVO> listWithCount(Long userId) {
        List<FavoriteFolderVO> result = new ArrayList<>();
        if (userId == null) {
            return result;
        }
        List<FavoriteFolder> folders = list(new LambdaQueryWrapper<FavoriteFolder>()
                .eq(FavoriteFolder::getUserId, userId)
                .orderByAsc(FavoriteFolder::getSortOrder)
                .orderByAsc(FavoriteFolder::getId));
        for (FavoriteFolder folder : folders) {
            FavoriteFolderVO vo = new FavoriteFolderVO();
            vo.setId(folder.getId());
            vo.setName(folder.getName());
            vo.setDescription(folder.getDescription());
            vo.setSortOrder(folder.getSortOrder());
            vo.setCreateTime(folder.getCreateTime());
            Long count = workFavoriteMapper.selectCount(new LambdaQueryWrapper<WorkFavorite>()
                    .eq(WorkFavorite::getUserId, userId)
                    .eq(WorkFavorite::getFolderId, folder.getId()));
            vo.setWorkCount(count == null ? 0L : count);
            result.add(vo);
        }
        return result;
    }

    @Override
    public int moveFavorites(List<Long> workIds, Long folderId, Long userId) {
        if (userId == null || workIds == null || workIds.isEmpty()) {
            return 0;
        }
        if (folderId != null) {
            requireOwned(folderId, userId);
        }
        workFavoriteMapper.update(null, new LambdaUpdateWrapper<WorkFavorite>()
                .eq(WorkFavorite::getUserId, userId)
                .in(WorkFavorite::getWorkId, workIds)
                .set(WorkFavorite::getFolderId, folderId));
        return workIds.size();
    }

    @Override
    public int cancelFavorites(List<Long> workIds, Long userId) {
        if (userId == null || workIds == null || workIds.isEmpty()) {
            return 0;
        }
        return workFavoriteMapper.delete(new LambdaQueryWrapper<WorkFavorite>()
                .eq(WorkFavorite::getUserId, userId)
                .in(WorkFavorite::getWorkId, workIds));
    }

    @Override
    public Map<String, Object> pageWorks(Long folderId, Long userId, long current, long size) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<PhotoWork> works = new ArrayList<>();
        long total = 0L;
        long ungrouped = 0L;
        if (userId != null) {
            LambdaQueryWrapper<WorkFavorite> wrapper = new LambdaQueryWrapper<WorkFavorite>()
                    .eq(WorkFavorite::getUserId, userId);
            if (folderId == null) {
                wrapper.isNull(WorkFavorite::getFolderId);
            } else {
                wrapper.eq(WorkFavorite::getFolderId, folderId);
            }
            wrapper.orderByDesc(WorkFavorite::getCreateTime);
            Page<WorkFavorite> page = workFavoriteMapper.selectPage(new Page<>(current, size), wrapper);
            total = page.getTotal();
            for (WorkFavorite favorite : page.getRecords()) {
                PhotoWork work = photoWorkService.getById(favorite.getWorkId());
                if (work != null && !Integer.valueOf(1).equals(work.getIsDelete())) {
                    works.add(work);
                }
            }
            Long count = workFavoriteMapper.selectCount(new LambdaQueryWrapper<WorkFavorite>()
                    .eq(WorkFavorite::getUserId, userId)
                    .isNull(WorkFavorite::getFolderId));
            ungrouped = count == null ? 0L : count;
        }
        data.put("records", works);
        data.put("total", total);
        data.put("ungroupedCount", ungrouped);
        return data;
    }
}
