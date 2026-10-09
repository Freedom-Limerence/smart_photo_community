package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.model.dto.ai.AiCatalogConfirmRequest;
import com.picture_backend.model.entity.PhotoAiTask;
import com.picture_backend.model.entity.PhotoAssetMeta;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.PhotoAiService;
import com.picture_backend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class PhotoAiController {
    @Resource private PhotoAiService photoAiService;
    @Resource private UserService userService;

    @PostMapping("/catalog")
    public BaseResponse<PhotoAiTask> catalog(@RequestParam Long pictureId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.catalog(pictureId, user.getId()));
    }

    /** 需求文档中的兼容路径：EXIF/摄影元数据任务统一走本地可替换 provider。 */
    @PostMapping("/task/exif")
    public BaseResponse<PhotoAiTask> exif(@RequestParam Long pictureId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.catalog(pictureId, user.getId()));
    }

    @GetMapping("/meta")
    public BaseResponse<PhotoAssetMeta> meta(@RequestParam Long pictureId) {
        return ResultUtils.success(photoAiService.getMeta(pictureId));
    }

    @GetMapping("/task/get")
    public BaseResponse<PhotoAiTask> task(@RequestParam Long id, HttpServletRequest request) {
        userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.getTask(id));
    }

    @PostMapping("/meta/confirm")
    public BaseResponse<PhotoAssetMeta> confirm(@RequestBody AiCatalogConfirmRequest requestBody, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        PhotoAssetMeta update = new PhotoAssetMeta();
        BeanUtils.copyProperties(requestBody, update);
        return ResultUtils.success(photoAiService.confirm(requestBody.getPictureId(), user.getId(), update));
    }

    @GetMapping("/search")
    public BaseResponse<List<Long>> search(@RequestParam String query, @RequestParam(defaultValue = "20") Integer topK, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.search(query, topK, user.getId()));
    }

    @GetMapping("/search/semantic")
    public BaseResponse<List<Long>> semanticSearch(@RequestParam String query, @RequestParam(defaultValue = "20") Integer topK, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.search(query, topK, user.getId()));
    }

    @PostMapping("/search/rebuild-index")
    public BaseResponse<Boolean> rebuildIndex(HttpServletRequest request) {
        userService.getLoginUser(request);
        return ResultUtils.success(true);
    }
}
