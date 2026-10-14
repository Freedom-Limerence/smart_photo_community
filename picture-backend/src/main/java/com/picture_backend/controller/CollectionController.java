package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.DeleteRequest;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.exception.ThrowUtils;
import com.picture_backend.model.dto.collection.CollectionAddRequest;
import com.picture_backend.model.dto.collection.CollectionPictureRequest;
import com.picture_backend.model.dto.collection.CollectionUpdateRequest;
import com.picture_backend.model.entity.PictureCollection;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.PictureCollectionVO;
import com.picture_backend.service.PictureCollectionService;
import com.picture_backend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 收藏夹接口
 */
@RestController
@RequestMapping("/collection")
public class CollectionController {

    @Resource
    private PictureCollectionService pictureCollectionService;

    @Resource
    private UserService userService;

    /**
     * 新增收藏夹
     */
    @PostMapping("/add")
    public BaseResponse<Long> addCollection(@RequestBody CollectionAddRequest collectionAddRequest,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(collectionAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newId = pictureCollectionService.addCollection(collectionAddRequest, loginUser);
        return ResultUtils.success(newId);
    }

    /**
     * 更新收藏夹
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateCollection(@RequestBody CollectionUpdateRequest collectionUpdateRequest,
                                                  HttpServletRequest request) {
        if (collectionUpdateRequest == null || collectionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 校验归属
        PictureCollection pictureCollection = pictureCollectionService.getById(collectionUpdateRequest.getId());
        pictureCollectionService.checkCollectionAuth(loginUser, pictureCollection);
        // 更新
        PictureCollection updateCollection = new PictureCollection();
        BeanUtils.copyProperties(collectionUpdateRequest, updateCollection);
        boolean result = pictureCollectionService.updateById(updateCollection);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除收藏夹
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteCollection(@RequestBody DeleteRequest deleteRequest,
                                                  HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 校验归属
        PictureCollection pictureCollection = pictureCollectionService.getById(deleteRequest.getId());
        pictureCollectionService.checkCollectionAuth(loginUser, pictureCollection);
        // 操作数据库
        boolean result = pictureCollectionService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取我的收藏夹列表（含图片数量）
     */
    @GetMapping("/list/my")
    public BaseResponse<List<PictureCollectionVO>> listMyCollections(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureCollectionService.listMyCollections(loginUser));
    }

    /**
     * 收藏图片
     */
    @PostMapping("/picture/add")
    public BaseResponse<Boolean> collectPicture(@RequestBody CollectionPictureRequest collectionPictureRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(collectionPictureRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureCollectionService.collectPicture(collectionPictureRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 取消收藏图片
     */
    @PostMapping("/picture/remove")
    public BaseResponse<Boolean> uncollectPicture(@RequestBody CollectionPictureRequest collectionPictureRequest,
                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(collectionPictureRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureCollectionService.uncollectPicture(collectionPictureRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取收藏夹内的图片 id 列表
     */
    @GetMapping("/picture/list")
    public BaseResponse<List<Long>> listCollectionPictures(long collectionId, HttpServletRequest request) {
        ThrowUtils.throwIf(collectionId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureCollectionService.listPictureIds(collectionId, loginUser));
    }
}
