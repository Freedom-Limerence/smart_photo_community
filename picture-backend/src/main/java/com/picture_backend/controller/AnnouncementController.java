package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.model.entity.Announcement;
import com.picture_backend.service.AnnouncementService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 公告（前台可见） */
@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    @Resource private AnnouncementService announcementService;

    /** 最新公告，前台顶部展示 */
    @GetMapping("/latest")
    public BaseResponse<List<Announcement>> latest(@RequestParam(defaultValue = "3") Integer limit) {
        return ResultUtils.success(announcementService.latest(limit));
    }
}
