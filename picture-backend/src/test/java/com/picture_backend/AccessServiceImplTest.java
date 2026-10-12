package com.picture_backend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.mapper.DownloadRecordMapper;
import com.picture_backend.mapper.PhotoWorkPictureMapper;
import com.picture_backend.mapper.SpaceUserMapper;
import com.picture_backend.model.dto.access.DownloadAuthorizeRequest;
import com.picture_backend.model.dto.access.InviteCreateRequest;
import com.picture_backend.model.entity.DownloadRecord;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.PhotoWorkPicture;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.model.entity.SpaceUser;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.impl.AccessServiceImpl;
import com.picture_backend.service.PictureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessServiceImplTest {

    @InjectMocks
    private AccessServiceImpl accessService;

    @Mock
    private DownloadRecordMapper downloadRecordMapper;

    @Mock
    private PhotoWorkPictureMapper photoWorkPictureMapper;

    @Mock
    private SpaceUserMapper spaceUserMapper;

    @Mock
    private PhotoWorkService photoWorkService;

    @Mock
    private PictureService pictureService;

    @Test
    void createInviteRejectsAdminRole() {
        InviteCreateRequest request = new InviteCreateRequest();
        request.setSpaceId(1L);
        request.setSpaceRole("admin");

        assertThrows(BusinessException.class, () -> accessService.createInvite(request, 2L));
    }

    @Test
    void authorizeDownloadRejectsReusingRequestForAnotherPicture() {
        DownloadRecord old = new DownloadRecord();
        old.setUserId(7L);
        old.setPictureId(11L);
        old.setWorkId(22L);
        old.setRequestId("request-1");
        old.setDownloadToken("token");
        when(downloadRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(old);

        DownloadAuthorizeRequest request = new DownloadAuthorizeRequest();
        request.setRequestId("request-1");
        request.setPictureId(12L);
        request.setWorkId(22L);

        assertThrows(BusinessException.class, () -> accessService.authorizeDownload(request, 7L));
    }

    @Test
    void authorizeDownloadRequiresWorkContextForRestrictedPublishedWork() {
        when(downloadRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        Picture picture = new Picture();
        picture.setId(12L);
        picture.setUserId(1L);
        picture.setReviewStatus(1);
        when(pictureService.getById(12L)).thenReturn(picture);

        PhotoWorkPicture relation = new PhotoWorkPicture();
        relation.setWorkId(22L);
        relation.setPictureId(12L);
        when(photoWorkPictureMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(relation));
        PhotoWork work = new PhotoWork();
        work.setId(22L);
        work.setAuthorId(1L);
        work.setStatus("PUBLISHED");
        work.setVisibility("FOLLOWER_ONLY");
        work.setDownloadPolicy("QUOTA_DOWNLOAD");
        when(photoWorkService.getById(22L)).thenReturn(work);

        DownloadAuthorizeRequest request = new DownloadAuthorizeRequest();
        request.setRequestId("request-2");
        request.setPictureId(12L);

        assertThrows(BusinessException.class, () -> accessService.authorizeDownload(request, 7L));
    }

    @Test
    void teamViewerCannotDownloadOriginalPhoto() {
        when(downloadRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        Picture picture = new Picture();
        picture.setId(12L);
        picture.setUserId(1L);
        picture.setSpaceId(5L);
        when(pictureService.getById(12L)).thenReturn(picture);
        when(photoWorkPictureMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        SpaceUser viewer = new SpaceUser();
        viewer.setSpaceId(5L);
        viewer.setUserId(7L);
        viewer.setSpaceRole("viewer");
        when(spaceUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(viewer);

        DownloadAuthorizeRequest request = new DownloadAuthorizeRequest();
        request.setRequestId("team-viewer-download");
        request.setPictureId(12L);

        assertThrows(BusinessException.class, () -> accessService.authorizeDownload(request, 7L));
    }
}
