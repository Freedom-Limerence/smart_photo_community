package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.picture_backend.model.dto.collection.CollectionAddRequest;
import com.picture_backend.model.dto.collection.CollectionPictureRequest;
import com.picture_backend.model.entity.PictureCollection;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.PictureCollectionVO;

import java.util.List;

/**
 * 收藏夹服务
 */
public interface PictureCollectionService extends IService<PictureCollection> {

    /**
     * 新增收藏夹
     *
     * @return 新增收藏夹 id
     */
    long addCollection(CollectionAddRequest collectionAddRequest, User loginUser);

    /**
     * 校验收藏夹归属（仅本人或管理员可操作）
     */
    PictureCollection checkCollectionAuth(User loginUser, PictureCollection pictureCollection);

    /**
     * 获取用户的收藏夹列表（含图片数量）
     */
    List<PictureCollectionVO> listMyCollections(User loginUser);

    /**
     * 收藏图片
     */
    void collectPicture(CollectionPictureRequest collectionPictureRequest, User loginUser);

    /**
     * 取消收藏图片
     */
    void uncollectPicture(CollectionPictureRequest collectionPictureRequest, User loginUser);

    /**
     * 获取收藏夹内的图片 id 列表（按收藏时间倒序）
     */
    List<Long> listPictureIds(long collectionId, User loginUser);
}
