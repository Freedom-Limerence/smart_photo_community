package com.picture_backend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.constant.UserConstant;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.CollectionPictureMapper;
import com.picture_backend.mapper.PictureCollectionMapper;
import com.picture_backend.model.dto.collection.CollectionAddRequest;
import com.picture_backend.model.dto.collection.CollectionPictureRequest;
import com.picture_backend.model.entity.CollectionPicture;
import com.picture_backend.model.entity.PictureCollection;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.PictureCollectionVO;
import com.picture_backend.service.PictureCollectionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏夹服务实现
 */
@Service
public class PictureCollectionServiceImpl extends ServiceImpl<PictureCollectionMapper, PictureCollection> implements PictureCollectionService {

    @Resource
    private CollectionPictureMapper collectionPictureMapper;

    @Override
    public long addCollection(CollectionAddRequest collectionAddRequest, User loginUser) {
        if (collectionAddRequest == null || StrUtil.isBlank(collectionAddRequest.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收藏夹名称不能为空");
        }
        PictureCollection pictureCollection = new PictureCollection();
        BeanUtils.copyProperties(collectionAddRequest, pictureCollection);
        pictureCollection.setUserId(loginUser.getId());
        boolean result = this.save(pictureCollection);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建失败");
        }
        return pictureCollection.getId();
    }

    @Override
    public PictureCollection checkCollectionAuth(User loginUser, PictureCollection pictureCollection) {
        if (pictureCollection == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "收藏夹不存在");
        }
        // 仅本人或管理员可操作
        if (!pictureCollection.getUserId().equals(loginUser.getId())
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return pictureCollection;
    }

    @Override
    public List<PictureCollectionVO> listMyCollections(User loginUser) {
        List<PictureCollection> collectionList = this.lambdaQuery()
                .eq(PictureCollection::getUserId, loginUser.getId())
                .orderByDesc(PictureCollection::getCreateTime)
                .list();
        List<PictureCollectionVO> voList = collectionList.stream()
                .map(PictureCollectionVO::objToVo)
                .collect(Collectors.toList());
        // 批量统计图片数量
        if (!voList.isEmpty()) {
            List<Long> collectionIds = voList.stream().map(PictureCollectionVO::getId).collect(Collectors.toList());
            List<CollectionPicture> relations = collectionPictureMapper.selectList(
                    new QueryWrapper<CollectionPicture>().in("collectionId", collectionIds));
            Map<Long, Long> countMap = relations.stream()
                    .collect(Collectors.groupingBy(CollectionPicture::getCollectionId, Collectors.counting()));
            voList.forEach(vo -> vo.setPictureCount(countMap.getOrDefault(vo.getId(), 0L)));
        }
        return voList;
    }

    @Override
    public void collectPicture(CollectionPictureRequest collectionPictureRequest, User loginUser) {
        Long collectionId = collectionPictureRequest.getCollectionId();
        Long pictureId = collectionPictureRequest.getPictureId();
        if (collectionId == null || collectionId <= 0 || pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验收藏夹归属
        PictureCollection pictureCollection = this.getById(collectionId);
        this.checkCollectionAuth(loginUser, pictureCollection);
        // 已收藏则幂等返回
        Long exists = collectionPictureMapper.selectCount(new QueryWrapper<CollectionPicture>()
                .eq("collectionId", collectionId)
                .eq("pictureId", pictureId));
        if (exists != null && exists > 0) {
            return;
        }
        CollectionPicture collectionPicture = new CollectionPicture();
        collectionPicture.setCollectionId(collectionId);
        collectionPicture.setPictureId(pictureId);
        int result = collectionPictureMapper.insert(collectionPicture);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "收藏失败");
        }
    }

    @Override
    public void uncollectPicture(CollectionPictureRequest collectionPictureRequest, User loginUser) {
        Long collectionId = collectionPictureRequest.getCollectionId();
        Long pictureId = collectionPictureRequest.getPictureId();
        if (collectionId == null || collectionId <= 0 || pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验收藏夹归属
        PictureCollection pictureCollection = this.getById(collectionId);
        this.checkCollectionAuth(loginUser, pictureCollection);
        collectionPictureMapper.delete(new QueryWrapper<CollectionPicture>()
                .eq("collectionId", collectionId)
                .eq("pictureId", pictureId));
    }

    @Override
    public List<Long> listPictureIds(long collectionId, User loginUser) {
        // 校验收藏夹归属
        PictureCollection pictureCollection = this.getById(collectionId);
        this.checkCollectionAuth(loginUser, pictureCollection);
        return collectionPictureMapper.selectList(new QueryWrapper<CollectionPicture>()
                        .eq("collectionId", collectionId)
                        .orderByDesc("createTime"))
                .stream()
                .map(CollectionPicture::getPictureId)
                .collect(Collectors.toList());
    }
}
