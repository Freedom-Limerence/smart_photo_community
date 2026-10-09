package com.picture_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.annotation.AuthCheck;
import com.picture_backend.annotation.LogRecord;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.DeleteRequest;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.constant.UserConstant;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.exception.ThrowUtils;
import com.picture_backend.model.dto.exhibition.ExhibitionAddRequest;
import com.picture_backend.model.dto.exhibition.ExhibitionPictureRequest;
import com.picture_backend.model.dto.exhibition.ExhibitionQueryRequest;
import com.picture_backend.model.dto.exhibition.ExhibitionUpdateRequest;
import com.picture_backend.model.dto.exhibition.ExhibitionWorkRequest;
import com.picture_backend.model.entity.Exhibition;
import com.picture_backend.model.entity.ExhibitionPicture;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.entity.ExhibitionWork;
import com.picture_backend.model.vo.ExhibitionVO;
import com.picture_backend.service.ExhibitionService;
import com.picture_backend.service.UserService;
import com.picture_backend.service.ExhibitionWorkService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 专题展览接口
 */
@RestController
@RequestMapping("/exhibition")
public class ExhibitionController {

    @Resource
    private ExhibitionService exhibitionService;

    @Resource
    private UserService userService;

    @Resource
    private ExhibitionWorkService exhibitionWorkService;

    // region 管理端

    /**
     * 新增展览（仅管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addExhibition(@RequestBody ExhibitionAddRequest exhibitionAddRequest,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(exhibitionAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newId = exhibitionService.addExhibition(exhibitionAddRequest, loginUser);
        return ResultUtils.success(newId);
    }

    /**
     * 更新展览（仅管理员）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateExhibition(@RequestBody ExhibitionUpdateRequest exhibitionUpdateRequest) {
        if (exhibitionUpdateRequest == null || exhibitionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Exhibition exhibition = new Exhibition();
        BeanUtils.copyProperties(exhibitionUpdateRequest, exhibition);
        // 校验
        exhibitionService.validExhibition(exhibition, false);
        // 判断是否存在
        long id = exhibitionUpdateRequest.getId();
        Exhibition oldExhibition = exhibitionService.getById(id);
        ThrowUtils.throwIf(oldExhibition == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = exhibitionService.updateById(exhibition);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除展览（仅管理员）
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteExhibition(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        Exhibition oldExhibition = exhibitionService.getById(id);
        ThrowUtils.throwIf(oldExhibition == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = exhibitionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 发布展览（仅管理员）
     */
    @PostMapping("/publish")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @LogRecord(action = "发布展览", module = "展览")
    public BaseResponse<Boolean> publishExhibition(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        exhibitionService.publishExhibition(deleteRequest.getId(), 1, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 下线展览（仅管理员）
     */
    @PostMapping("/offline")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @LogRecord(action = "下线展览", module = "展览")
    public BaseResponse<Boolean> offlineExhibition(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        exhibitionService.publishExhibition(deleteRequest.getId(), 2, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 展览添加展品（仅管理员）
     */
    @PostMapping("/picture/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addPictures(@RequestBody ExhibitionPictureRequest exhibitionPictureRequest) {
        ThrowUtils.throwIf(exhibitionPictureRequest == null, ErrorCode.PARAMS_ERROR);
        exhibitionService.addPictures(exhibitionPictureRequest);
        return ResultUtils.success(true);
    }

    /**
     * 展览移除展品（仅管理员）
     */
    @PostMapping("/picture/remove")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> removePictures(@RequestBody ExhibitionPictureRequest exhibitionPictureRequest) {
        ThrowUtils.throwIf(exhibitionPictureRequest == null, ErrorCode.PARAMS_ERROR);
        exhibitionService.removePictures(exhibitionPictureRequest);
        return ResultUtils.success(true);
    }

    /**
     * 展品排序（仅管理员）
     */
    @PostMapping("/picture/sort")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> sortPictures(@RequestBody ExhibitionPictureRequest exhibitionPictureRequest) {
        ThrowUtils.throwIf(exhibitionPictureRequest == null, ErrorCode.PARAMS_ERROR);
        exhibitionService.sortPictures(exhibitionPictureRequest);
        return ResultUtils.success(true);
    }

    // endregion

    @PostMapping("/work/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addWorks(@RequestBody ExhibitionWorkRequest body) { exhibitionWorkService.addWorks(body); return ResultUtils.success(true); }

    @PostMapping("/work/remove")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> removeWorks(@RequestBody ExhibitionWorkRequest body) { exhibitionWorkService.removeWorks(body); return ResultUtils.success(true); }

    @PostMapping("/work/sort")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> sortWorks(@RequestBody ExhibitionWorkRequest body) { exhibitionWorkService.sortWorks(body); return ResultUtils.success(true); }

    @GetMapping("/work/list")
    public BaseResponse<List<ExhibitionWork>> listWorks(@RequestParam Long exhibitionId, HttpServletRequest request) {
        Exhibition exhibition = exhibitionService.getById(exhibitionId);
        ThrowUtils.throwIf(exhibition == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(exhibition.getStatus() != 1 && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()), ErrorCode.NO_AUTH_ERROR, "该展览未发布");
        return ResultUtils.success(exhibitionWorkService.listWorks(exhibitionId));
    }

    // region 用户端

    /**
     * 分页获取展览列表（用户端只返回已发布）
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ExhibitionVO>> listExhibitionByPage(@RequestBody ExhibitionQueryRequest exhibitionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(exhibitionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = exhibitionQueryRequest.getCurrent();
        long size = exhibitionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页最多 20 条");
        Page<Exhibition> exhibitionPage = exhibitionService.page(
                new Page<>(current, size),
                exhibitionService.getQueryWrapper(exhibitionQueryRequest, request));
        return ResultUtils.success(exhibitionService.getExhibitionVOPage(exhibitionPage));
    }

    /**
     * 根据 id 获取展览详情（封装类，未发布的仅管理员可见）
     */
    @GetMapping("/get/vo")
    public BaseResponse<ExhibitionVO> getExhibitionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Exhibition exhibition = exhibitionService.getById(id);
        ThrowUtils.throwIf(exhibition == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        ThrowUtils.throwIf(!isAdmin && exhibition.getStatus() != 1, ErrorCode.NO_AUTH_ERROR, "该展览未发布");
        // 浏览量 +1（草稿/下线状态不计）
        if (exhibition.getStatus() == 1) {
            Exhibition updateExhibition = new Exhibition();
            updateExhibition.setId(id);
            updateExhibition.setViewCount((exhibition.getViewCount() == null ? 0 : exhibition.getViewCount()) + 1);
            exhibitionService.updateById(updateExhibition);
            exhibition.setViewCount(updateExhibition.getViewCount());
        }
        return ResultUtils.success(ExhibitionVO.objToVo(exhibition));
    }

    /**
     * 获取展览的展品列表（按 sortOrder 升序）
     */
    @GetMapping("/picture/list")
    public BaseResponse<List<ExhibitionPicture>> listExhibitionPictures(long exhibitionId) {
        ThrowUtils.throwIf(exhibitionId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(exhibitionService.listPictures(exhibitionId));
    }

    // endregion
}
