package com.picture_backend.service.impl;

import com.picture_backend.mapper.SysPermissionMapper;
import com.picture_backend.mapper.SysRoleMapper;
import com.picture_backend.mapper.SysRolePermissionMapper;
import com.picture_backend.model.entity.SysRolePermission;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * RBAC 权限判定单元测试：验证「策展人」与「管理员」的能力边界。
 */
@ExtendWith(MockitoExtension.class)
class RbacServiceImplTest {

    @Mock private SysRoleMapper roleMapper;
    @Mock private SysPermissionMapper permissionMapper;
    @Mock private SysRolePermissionMapper rolePermissionMapper;
    @Mock private UserService userService;

    private RbacServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RbacServiceImpl();
        ReflectionTestUtils.setField(service, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "permissionMapper", permissionMapper);
        ReflectionTestUtils.setField(service, "rolePermissionMapper", rolePermissionMapper);
        ReflectionTestUtils.setField(service, "userService", userService);
        when(rolePermissionMapper.selectList(any())).thenReturn(rows());
    }

    private List<SysRolePermission> rows() {
        List<SysRolePermission> list = new ArrayList<>();
        list.add(row("admin", "rbac:manage"));
        list.add(row("admin", "content:review"));
        list.add(row("curator", "content:review"));
        list.add(row("curator", "report:manage"));
        list.add(row("user", "none"));
        return list;
    }

    private SysRolePermission row(String role, String perm) {
        SysRolePermission item = new SysRolePermission();
        item.setRoleCode(role);
        item.setPermCode(perm);
        return item;
    }

    private User user(String role) {
        User user = new User();
        user.setId(1L);
        user.setUserRole(role);
        return user;
    }

    @Test
    void adminHasManagementPermission() {
        assertTrue(service.hasPermission(user("admin"), "rbac:manage"));
        assertTrue(service.hasPermission(user("admin"), "content:review"));
    }

    @Test
    void curatorCanReviewAndHandleReportsButNotManageRbac() {
        assertTrue(service.hasPermission(user("curator"), "content:review"));
        assertTrue(service.hasPermission(user("curator"), "report:manage"));
        assertFalse(service.hasPermission(user("curator"), "rbac:manage"));
        assertFalse(service.hasPermission(user("curator"), "system:monitor"));
    }

    @Test
    void normalUserHasNoBackendPermission() {
        assertFalse(service.hasPermission(user("user"), "content:review"));
        assertFalse(service.hasPermission(user("banned"), "content:review"));
    }

    @Test
    void unknownRoleAndNullUserAreDenied() {
        assertFalse(service.hasPermission(user("not-exist"), "content:review"));
        assertFalse(service.hasPermission(null, "content:review"));
        assertTrue(service.getPermissionCodes("not-exist").isEmpty());
        assertTrue(service.getPermissionCodes(null).isEmpty());
    }
}
