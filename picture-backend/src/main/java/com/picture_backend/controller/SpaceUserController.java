package com.picture_backend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.DeleteRequest;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.exception.ThrowUtils;
import com.picture_backend.manager.auth.annotation.SaSpaceCheckPermission;
import com.picture_backend.manager.auth.model.SpaceUserPermissionConstant;
import com.picture_backend.model.dto.spaceuser.SpaceUserAddRequest;
import com.picture_backend.model.dto.spaceuser.SpaceUserEditRequest;
import com.picture_backend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.picture_backend.model.enums.SpaceRoleEnum;
import com.picture_backend.model.entity.SpaceUser;
import com.picture_backend.model.entity.Space;
import com.picture_backend.model.enums.SpaceTypeEnum;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.SpaceUserVO;
import com.picture_backend.service.SpaceUserService;
import com.picture_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员管理
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    /**
     * 添加成员到空间
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        Space targetSpace = spaceUserService.getSpaceById(spaceUserAddRequest.getSpaceId());
        ThrowUtils.throwIf(targetSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(SpaceTypeEnum.TEAM.getValue() != targetSpace.getSpaceType(), ErrorCode.PARAMS_ERROR, "只有团队空间可以添加成员");
        long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 从空间移除成员
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        Space targetSpace = spaceUserService.getSpaceById(oldSpaceUser.getSpaceId());
        ThrowUtils.throwIf(targetSpace != null && oldSpaceUser.getUserId().equals(targetSpace.getUserId()),
                ErrorCode.OPERATION_ERROR, "不能移除空间创建者");
        if (targetSpace != null && SpaceRoleEnum.ADMIN.getValue().equals(oldSpaceUser.getSpaceRole())) {
            long adminCount = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, oldSpaceUser.getSpaceId())
                    .eq(SpaceUser::getSpaceRole, SpaceRoleEnum.ADMIN.getValue())
                    .count();
            ThrowUtils.throwIf(adminCount <= 1, ErrorCode.OPERATION_ERROR, "团队至少需要保留一名管理员");
        }
        // 操作数据库
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                               HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        // 数据校验
        spaceUserService.validSpaceUser(spaceUser, false);
        // 判断是否存在
        long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        Space targetSpace = spaceUserService.getSpaceById(oldSpaceUser.getSpaceId());
        String requestedRole = spaceUserEditRequest.getSpaceRole();
        if (targetSpace != null && oldSpaceUser.getUserId().equals(targetSpace.getUserId())
                && requestedRole != null && !SpaceRoleEnum.ADMIN.getValue().equals(requestedRole)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能修改空间创建者的管理员角色");
        }
        if (targetSpace != null && SpaceRoleEnum.ADMIN.getValue().equals(oldSpaceUser.getSpaceRole())
                && requestedRole != null && !SpaceRoleEnum.ADMIN.getValue().equals(requestedRole)) {
            long adminCount = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, oldSpaceUser.getSpaceId())
                    .eq(SpaceUser::getSpaceRole, SpaceRoleEnum.ADMIN.getValue())
                    .count();
            ThrowUtils.throwIf(adminCount <= 1, ErrorCode.OPERATION_ERROR, "团队至少需要保留一名管理员");
        }
        // 操作数据库
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }
}
