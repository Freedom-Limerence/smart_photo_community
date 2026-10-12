package com.picture_backend;

import com.picture_backend.manager.auth.SpaceUserAuthManager;
import com.picture_backend.manager.auth.model.SpaceUserPermissionConstant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceUserAuthManagerTest {

    private final SpaceUserAuthManager authManager = new SpaceUserAuthManager();

    @Test
    void viewerOnlyHasViewPermission() {
        List<String> permissions = authManager.getPermissionsByRole("viewer");

        assertTrue(permissions.contains(SpaceUserPermissionConstant.PICTURE_VIEW));
        assertFalse(permissions.contains(SpaceUserPermissionConstant.PICTURE_UPLOAD));
        assertFalse(permissions.contains(SpaceUserPermissionConstant.PICTURE_EDIT));
        assertFalse(permissions.contains(SpaceUserPermissionConstant.PICTURE_DELETE));
        assertFalse(permissions.contains(SpaceUserPermissionConstant.SPACE_USER_MANAGE));
    }

    @Test
    void editorCanManagePicturesButNotMembers() {
        List<String> permissions = authManager.getPermissionsByRole("editor");

        assertTrue(permissions.contains(SpaceUserPermissionConstant.PICTURE_VIEW));
        assertTrue(permissions.contains(SpaceUserPermissionConstant.PICTURE_UPLOAD));
        assertTrue(permissions.contains(SpaceUserPermissionConstant.PICTURE_EDIT));
        assertTrue(permissions.contains(SpaceUserPermissionConstant.PICTURE_DELETE));
        assertFalse(permissions.contains(SpaceUserPermissionConstant.SPACE_USER_MANAGE));
    }

    @Test
    void unknownRoleHasNoPermission() {
        assertTrue(authManager.getPermissionsByRole("invalid").isEmpty());
    }
}
