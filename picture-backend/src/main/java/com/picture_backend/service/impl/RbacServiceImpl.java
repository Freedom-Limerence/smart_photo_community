package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.SysPermissionMapper;
import com.picture_backend.mapper.SysRoleMapper;
import com.picture_backend.mapper.SysRolePermissionMapper;
import com.picture_backend.model.entity.SysPermission;
import com.picture_backend.model.entity.SysRole;
import com.picture_backend.model.entity.SysRolePermission;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.RbacService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RBAC 实现：权限来自数据库（sys_role_permission），带 30 秒本地缓存。
 */
@Slf4j
@Service
public class RbacServiceImpl implements RbacService {

    private static final long CACHE_TTL_MS = 30_000L;

    @Resource private SysRoleMapper roleMapper;
    @Resource private SysPermissionMapper permissionMapper;
    @Resource private SysRolePermissionMapper rolePermissionMapper;
    @Resource private UserService userService;

    private volatile Map<String, List<String>> cache;
    private volatile long cacheAt;

    private Map<String, List<String>> matrix() {
        if (cache == null || System.currentTimeMillis() - cacheAt > CACHE_TTL_MS) {
            Map<String, List<String>> map = new HashMap<>();
            for (SysRolePermission rp : rolePermissionMapper.selectList(null)) {
                map.computeIfAbsent(rp.getRoleCode(), k -> new ArrayList<>()).add(rp.getPermCode());
            }
            cache = map;
            cacheAt = System.currentTimeMillis();
        }
        return cache;
    }

    @Override
    public List<SysRole> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSortOrder));
    }

    @Override
    public List<SysPermission> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortOrder));
    }

    @Override
    public Map<String, List<String>> rolePermissionMatrix() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (SysRole role : listRoles()) {
            result.put(role.getRoleCode(), new ArrayList<>(getPermissionCodes(role.getRoleCode())));
        }
        return result;
    }

    @Override
    public List<String> getPermissionCodes(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return Collections.emptyList();
        }
        return matrix().getOrDefault(roleCode, Collections.emptyList());
    }

    @Override
    public boolean hasPermission(User user, String permCode) {
        if (user == null || !StringUtils.hasText(permCode)) {
            return false;
        }
        List<String> codes = getPermissionCodes(user.getUserRole());
        return codes.contains("*") || codes.contains(permCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRolePermissions(String roleCode, List<String> permCodes) {
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色编码不能为空");
        }
        SysRole role = roleMapper.selectById(roleCode);
        if (role == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色不存在：" + roleCode);
        }
        if ("admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "超级管理员权限不可修改");
        }
        List<String> valid = permCodes == null ? Collections.emptyList() : permCodes.stream().distinct().collect(Collectors.toList());
        if (!valid.isEmpty()) {
            long known = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>().in(SysPermission::getPermCode, valid));
            if (known != valid.size()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "包含未知权限编码");
            }
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleCode, roleCode));
        for (String code : valid) {
            SysRolePermission record = new SysRolePermission();
            record.setRoleCode(roleCode);
            record.setPermCode(code);
            rolePermissionMapper.insert(record);
        }
        evictCache();
        log.info("[RBAC] 角色 {} 权限更新为 {} 项", roleCode, valid.size());
    }

    @Override
    public void assignUserRole(Long userId, String roleCode) {
        if (userId == null || !StringUtils.hasText(roleCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不完整");
        }
        if (roleMapper.selectById(roleCode) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色不存在：" + roleCode);
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        User patch = new User();
        patch.setId(userId);
        patch.setUserRole(roleCode);
        userService.updateById(patch);
        log.info("[RBAC] 用户 {} 角色改为 {}", userId, roleCode);
    }

    @Override
    public void evictCache() {
        cache = null;
        cacheAt = 0L;
    }
}
