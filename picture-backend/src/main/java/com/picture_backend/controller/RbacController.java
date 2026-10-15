package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.model.entity.SysPermission;
import com.picture_backend.model.entity.SysRole;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.RbacService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色与权限（RBAC）管理接口：仅拥有 rbac:manage 权限的角色可操作。
 */
@RestController
@RequestMapping("/admin/rbac")
public class RbacController {

    @Resource private RbacService rbacService;
    @Resource private UserService userService;

    private User requireRbacManage(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        if (!rbacService.hasPermission(user, "rbac:manage")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少权限：rbac:manage");
        }
        return user;
    }

    /** 当前登录用户在后端的角色与权限（前端据此隐藏菜单） */
    @GetMapping("/my")
    public BaseResponse<Map<String, Object>> my(HttpServletRequest request) {
        User user = userService.getLoginUserPermitNull(request);
        Map<String, Object> data = new LinkedHashMap<>();
        if (user == null) {
            data.put("roleCode", null);
            data.put("permCodes", List.of());
            data.put("isAdmin", false);
            return ResultUtils.success(data);
        }
        List<String> codes = rbacService.getPermissionCodes(user.getUserRole());
        data.put("roleCode", user.getUserRole());
        data.put("permCodes", codes);
        data.put("isAdmin", codes.contains("rbac:manage"));
        return ResultUtils.success(data);
    }

    /** 角色列表 */
    @GetMapping("/roles")
    public BaseResponse<List<SysRole>> roles(HttpServletRequest request) {
        requireRbacManage(request);
        return ResultUtils.success(rbacService.listRoles());
    }

    /** 权限点列表 */
    @GetMapping("/permissions")
    public BaseResponse<List<SysPermission>> permissions(HttpServletRequest request) {
        requireRbacManage(request);
        return ResultUtils.success(rbacService.listPermissions());
    }

    /** 权限矩阵：角色 + 权限 + 映射（一次请求拿全，供权限矩阵界面使用） */
    @GetMapping("/matrix")
    public BaseResponse<Map<String, Object>> matrix(HttpServletRequest request) {
        requireRbacManage(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roles", rbacService.listRoles());
        data.put("permissions", rbacService.listPermissions());
        data.put("mapping", rbacService.rolePermissionMatrix());
        return ResultUtils.success(data);
    }

    /** 覆盖式更新角色权限 */
    @PostMapping("/role/permissions")
    public BaseResponse<Boolean> updateRolePermissions(@RequestBody RolePermissionRequest body,
                                                       HttpServletRequest request) {
        requireRbacManage(request);
        if (body == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        rbacService.updateRolePermissions(body.getRoleCode(), body.getPermCodes());
        return ResultUtils.success(true);
    }

    /** 给用户分配角色 */
    @PostMapping("/user/role")
    public BaseResponse<Boolean> assignUserRole(@RequestBody UserRoleRequest body, HttpServletRequest request) {
        requireRbacManage(request);
        if (body == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        rbacService.assignUserRole(body.getUserId(), body.getRoleCode());
        return ResultUtils.success(true);
    }

    @Data
    public static class RolePermissionRequest {
        private String roleCode;
        private List<String> permCodes;
    }

    @Data
    public static class UserRoleRequest {
        private Long userId;
        private String roleCode;
    }
}
