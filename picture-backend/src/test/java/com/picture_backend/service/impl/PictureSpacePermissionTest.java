package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.manager.auth.SpaceUserAuthManager;
import com.picture_backend.mapper.SpaceUserMapper;
import com.picture_backend.model.entity.Space;
import com.picture_backend.model.entity.SpaceUser;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.enums.SpaceTypeEnum;
import com.picture_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PictureSpacePermissionTest {

    @Mock
    private UserService userService;

    @Mock
    private SpaceUserMapper spaceUserMapper;

    private PictureServiceImpl pictureService;
    private final User memberUser = new User();
    private final Space teamSpace = new Space();

    @BeforeEach
    void setUp() {
        pictureService = new PictureServiceImpl();
        ReflectionTestUtils.setField(pictureService, "userService", userService);
        ReflectionTestUtils.setField(pictureService, "spaceUserMapper", spaceUserMapper);
        ReflectionTestUtils.setField(pictureService, "spaceUserAuthManager", new SpaceUserAuthManager());
        memberUser.setId(20L);
        teamSpace.setId(10L);
        teamSpace.setUserId(1L);
        teamSpace.setSpaceType(SpaceTypeEnum.TEAM.getValue());
        when(userService.isAdmin(memberUser)).thenReturn(false);
    }

    @Test
    void viewerCanViewButCannotUpload() {
        mockMembership("viewer");

        assertDoesNotThrow(() -> check(teamSpace, "picture:view"));
        assertThrows(BusinessException.class, () -> check(teamSpace, "picture:upload"));
    }

    @Test
    void editorCanUploadButCannotManageMembers() {
        mockMembership("editor");

        assertDoesNotThrow(() -> check(teamSpace, "picture:upload"));
        assertThrows(BusinessException.class, () -> check(teamSpace, "spaceUser:manage"));
    }

    @Test
    void adminCanManageMembersAndNonMemberHasNoAccess() {
        mockMembership("admin");
        assertDoesNotThrow(() -> check(teamSpace, "spaceUser:manage"));

        when(spaceUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThrows(BusinessException.class, () -> check(teamSpace, "picture:view"));
    }

    private void mockMembership(String role) {
        SpaceUser membership = new SpaceUser();
        membership.setSpaceId(teamSpace.getId());
        membership.setUserId(memberUser.getId());
        membership.setSpaceRole(role);
        when(spaceUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(membership);
    }

    private void check(Space space, String permission) {
        ReflectionTestUtils.invokeMethod(pictureService, "ensureSpacePermission", space, memberUser, permission);
    }
}
