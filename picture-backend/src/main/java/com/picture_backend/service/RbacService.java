package com.picture_backend.service;

import com.picture_backend.model.entity.SysPermission;
import com.picture_backend.model.entity.SysRole;
import com.picture_backend.model.entity.User;

import java.util.List;
import java.util.Map;

/** RBAC：角色 / 权限 / 角色权限矩阵 */
public interface RbacService {
    List<SysRole> listRoles();

    List<SysPermission> listPermissions();

    /** roleCode -> permCodes */
    Map<String, List<String>> rolePermissionMatrix();

    List<String> getPermissionCodes(String roleCode);

    boolean hasPermission(User user, String permCode);

    /** 覆盖式更新某角色的权限集合（内置超级管理员 admin 不允许修改） */
    void updateRolePermissions(String roleCode, List<String> permCodes);

    /** 给用户分配角色 */
    void assignUserRole(Long userId, String roleCode);

    void evictCache();
}
