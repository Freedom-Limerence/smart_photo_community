package com.picture_backend.service.impl;

import com.picture_backend.mapper.PictureMapper;
import com.picture_backend.mapper.SysConfigMapper;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.model.entity.SysConfig;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.AutoReviewService;
import com.picture_backend.service.NotificationService;
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
 * 机审规则引擎单元测试：每条规则都要能被独立验证。
 */
@ExtendWith(MockitoExtension.class)
class AutoReviewServiceImplTest {

    @Mock private SysConfigMapper sysConfigMapper;
    @Mock private PictureMapper pictureMapper;
    @Mock private NotificationService notificationService;

    private AutoReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AutoReviewServiceImpl();
        ReflectionTestUtils.setField(service, "sysConfigMapper", sysConfigMapper);
        ReflectionTestUtils.setField(service, "pictureMapper", pictureMapper);
        ReflectionTestUtils.setField(service, "notificationService", notificationService);
        when(sysConfigMapper.selectList(any())).thenReturn(config());
    }

    private List<SysConfig> config() {
        List<SysConfig> list = new ArrayList<>();
        list.add(cfg("autoReview.enabled", "true"));
        list.add(cfg("autoReview.minWidth", "400"));
        list.add(cfg("autoReview.minHeight", "400"));
        list.add(cfg("autoReview.minHistory", "3"));
        list.add(cfg("autoReview.minPassRate", "0.8"));
        list.add(cfg("autoReview.burstWindowMinutes", "10"));
        list.add(cfg("autoReview.burstLimit", "15"));
        list.add(cfg("autoReview.bannedWords", "涉政,赌博,色情"));
        return list;
    }

    private SysConfig cfg(String key, String value) {
        SysConfig row = new SysConfig();
        row.setConfigKey(key);
        row.setConfigValue(value);
        return row;
    }

    private User author() {
        User user = new User();
        user.setId(1L);
        user.setUserRole("user");
        user.setUserName("测试作者");
        return user;
    }

    private Picture picture(String name, Integer width, Integer height) {
        Picture picture = new Picture();
        picture.setId(100L);
        picture.setUserId(1L);
        picture.setName(name);
        picture.setPicWidth(width);
        picture.setPicHeight(height);
        return picture;
    }

    @Test
    void firstRiskWordHitsConfiguredWord() {
        assertEquals("赌博", service.firstRiskWord("这里有赌博链接"));
        assertNull(service.firstRiskWord("一张安静的风景照"));
        assertNull(service.firstRiskWord(null));
    }

    @Test
    void workWithRiskyTextGoesToManualReview() {
        PhotoWork work = new PhotoWork();
        work.setId(9L);
        work.setTitle("赌博推广内容");
        AutoReviewService.Decision decision = service.decideWork(work, author());
        assertEquals(0, decision.getReviewStatus());
        assertEquals("TEXT_RISK", decision.getRule());
    }

    @Test
    void cleanWorkFromBannedAuthorGoesToManualReview() {
        User banned = author();
        banned.setUserRole("banned");
        PhotoWork work = new PhotoWork();
        work.setTitle("正常标题");
        AutoReviewService.Decision decision = service.decideWork(work, banned);
        assertEquals(0, decision.getReviewStatus());
        assertEquals("BANNED_AUTHOR", decision.getRule());
    }

    @Test
    void cleanWorkPassesAutomatically() {
        when(pictureMapper.selectCount(any())).thenReturn(3L);
        PhotoWork work = new PhotoWork();
        work.setTitle("晨雾中的山谷");
        work.setDescription("清晨在川西拍摄");
        AutoReviewService.Decision decision = service.decideWork(work, author());
        assertEquals(1, decision.getReviewStatus());
        assertEquals("AUTO_PASS", decision.getRule());
    }

    @Test
    void lowResolutionPictureGoesToManualReview() {
        AutoReviewService.Decision decision = service.decide(picture("缩略图", 120, 90), author());
        assertEquals(0, decision.getReviewStatus());
        assertEquals("LOW_RESOLUTION", decision.getRule());
    }

    @Test
    void newAuthorWithoutHistoryGoesToManualReview() {
        when(pictureMapper.selectCount(any())).thenReturn(1L);
        AutoReviewService.Decision decision = service.decide(picture("风景", 1920, 1080), author());
        assertEquals(0, decision.getReviewStatus());
        assertEquals("NEW_AUTHOR", decision.getRule());
    }

    @Test
    void trustworthyAuthorPassesAutomatically() {
        when(pictureMapper.selectCount(any())).thenReturn(5L);
        AutoReviewService.Decision decision = service.decide(picture("风景", 1920, 1080), author());
        assertEquals(1, decision.getReviewStatus());
        assertEquals("AUTO_PASS", decision.getRule());
        assertTrue(decision.getMessage().contains("机审通过"));
    }

    @Test
    void lowPassRateGoesToManualReview() {
        when(pictureMapper.selectCount(any())).thenReturn(5L, 1L);
        AutoReviewService.Decision decision = service.decide(picture("风景", 1920, 1080), author());
        assertEquals(0, decision.getReviewStatus());
        assertEquals("LOW_PASS_RATE", decision.getRule());
    }

    @Test
    void burstUploadGoesToManualReview() {
        when(pictureMapper.selectCount(any())).thenReturn(5L, 5L, 99L);
        AutoReviewService.Decision decision = service.decide(picture("风景", 1920, 1080), author());
        assertEquals(0, decision.getReviewStatus());
        assertEquals("BURST_UPLOAD", decision.getRule());
    }
}
