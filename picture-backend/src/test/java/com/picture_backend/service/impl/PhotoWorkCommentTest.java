package com.picture_backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.mapper.PhotoWorkMapper;
import com.picture_backend.mapper.WorkCommentMapper;
import com.picture_backend.mapper.WorkCommentLikeMapper;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.entity.WorkComment;
import com.picture_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoWorkCommentTest {
    @Mock private PhotoWorkMapper workMapper;
    @Mock private WorkCommentMapper commentMapper;
    @Mock private WorkCommentLikeMapper commentLikeMapper;
    @Mock private UserService userService;

    private PhotoWorkServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PhotoWorkServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", workMapper);
        ReflectionTestUtils.setField(service, "workCommentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "workCommentLikeMapper", commentLikeMapper);
        ReflectionTestUtils.setField(service, "userService", userService);
    }

    @Test
    void privateCommentsRequireWorkVisibility() {
        PhotoWork work = work("PRIVATE");
        when(workMapper.selectById(10L)).thenReturn(work);

        assertThrows(BusinessException.class, () -> service.listComments(10L, null));
        assertThrows(BusinessException.class, () -> service.listComments(10L, 2L));
        verifyNoInteractions(commentMapper);
    }

    @Test
    void replyCannotReferenceCommentFromAnotherWork() {
        when(workMapper.selectById(10L)).thenReturn(work("PRIVATE"));
        WorkComment parent = new WorkComment();
        parent.setId(5L);
        parent.setWorkId(11L);
        parent.setStatus("VISIBLE");
        when(commentMapper.selectById(5L)).thenReturn(parent);

        assertThrows(BusinessException.class, () -> service.addComment(10L, 1L, 5L, "reply"));
        verify(commentMapper, never()).insert(any(WorkComment.class));
    }

    @Test
    void visibleCommentsIncludeOnlyPublicAuthorFields() throws Exception {
        when(workMapper.selectById(10L)).thenReturn(work("PUBLIC"));
        WorkComment comment = new WorkComment();
        comment.setId(5L);
        comment.setWorkId(10L);
        comment.setUserId(2L);
        when(commentMapper.selectList(any())).thenReturn(List.of(comment));

        User author = new User();
        author.setId(2L);
        author.setUserName("摄影师");
        author.setUserAvatar("https://example.test/avatar.png");
        author.setUserAccount("private-account");
        when(userService.listByIds(anyCollection())).thenReturn(List.of(author));

        List<WorkComment> comments = service.listComments(10L, null);
        assertEquals("摄影师", comments.get(0).getUser().getUserName());
        String json = new ObjectMapper().writeValueAsString(comments);
        assertTrue(json.contains("摄影师"));
        assertFalse(json.contains("private-account"));
        assertFalse(json.contains("userAccount"));
    }

    @Test
    void pendingReviewWorkCannotBeReadByVisitors() {
        PhotoWork work = work("PUBLIC");
        work.setReviewStatus(0);
        when(workMapper.selectById(10L)).thenReturn(work);

        assertThrows(BusinessException.class, () -> service.listComments(10L, null));
        assertThrows(BusinessException.class, () -> service.listComments(10L, 2L));
        verifyNoInteractions(commentMapper);
    }

    @Test
    void hiddenCommentsCannotReceiveLikes() {
        WorkComment comment = new WorkComment();
        comment.setId(5L);
        comment.setWorkId(10L);
        comment.setStatus("HIDDEN");
        when(commentMapper.selectById(5L)).thenReturn(comment);

        assertThrows(BusinessException.class, () -> service.likeComment(5L, 2L, true));
        verifyNoInteractions(commentLikeMapper);
    }

    private PhotoWork work(String visibility) {
        PhotoWork work = new PhotoWork();
        work.setId(10L);
        work.setAuthorId(1L);
        work.setStatus("PUBLISHED");
        work.setReviewStatus(1);
        work.setVisibility(visibility);
        return work;
    }
}
