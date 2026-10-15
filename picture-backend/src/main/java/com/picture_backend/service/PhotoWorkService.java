package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.model.dto.work.PhotoWorkRequest;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.WorkComment;

import java.util.List;

public interface PhotoWorkService {
    PhotoWork getById(Long workId);
    Long saveWork(PhotoWorkRequest request, Long userId);
    boolean publish(Long workId, Long userId, boolean publish);
    PhotoWork getVisible(Long workId, Long userId);
    void validateVisible(Long workId, Long userId);
    Page<PhotoWork> page(long current, long size, Long userId);

    /** 按作者分页（摄影师主页）：仅返回已发布作品，访客只看到公开可见的 */
    Page<PhotoWork> pageByAuthor(Long authorId, long current, long size, Long viewerId);
    boolean like(Long workId, Long userId, boolean like);
    boolean favorite(Long workId, Long userId, boolean favorite);
    List<Long> listFavoriteWorkIds(Long userId);
    WorkComment addComment(Long workId, Long userId, Long parentId, String content);
    List<WorkComment> listComments(Long workId);
    boolean follow(Long followingId, Long followerId, boolean follow);
}
