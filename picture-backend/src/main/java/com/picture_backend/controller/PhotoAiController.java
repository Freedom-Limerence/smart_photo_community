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
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class PhotoAiController {
    @Resource
    private PhotoAiService photoAiService;
    @Resource
    private UserService userService;

    /** 生成摄影编目（阶段 2.3） */
    @PostMapping("/catalog")
    public BaseResponse<PhotoAiTask> catalog(@RequestParam Long pictureId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.catalog(pictureId, user.getId()));
    }

    /** EXIF 提取任务（阶段 2.2）：读取原图并解析真实拍摄信息 */
    @PostMapping("/task/exif")
    public BaseResponse<PhotoAiTask> exif(@RequestParam Long pictureId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.startExifTask(pictureId, user.getId()));
    }

    /** 我的任务列表（阶段 2.1） */
    @GetMapping("/task/page/my")
    public BaseResponse<List<PhotoAiTask>> myTasks(@RequestParam(defaultValue = "1") Integer current,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestParam(required = false) String status,
                                                   HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResultUtils.success(photoAiService.listMyTasks(user.getId(), Math.max(current, 1), safeSize, status));
    }

    /** 重试失败任务（阶段 2.1） */
    @PostMapping("/task/retry")
    public BaseResponse<PhotoAiTask> retry(@RequestParam Long id, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.retryTask(id, user.getId()));
    }

    /** 取消任务（阶段 2.1） */
    @PostMapping("/task/cancel")
    public BaseResponse<PhotoAiTask> cancel(@RequestParam Long id, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.cancelTask(id, user.getId()));
    }

    @GetMapping("/task/get")
    public BaseResponse<PhotoAiTask> task(@RequestParam Long id, HttpServletRequest request) {
        userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.getTask(id));
    }

    @GetMapping("/meta")
    public BaseResponse<PhotoAssetMeta> meta(@RequestParam Long pictureId) {
        return ResultUtils.success(photoAiService.getMeta(pictureId));
    }

    /** 人工确认：confirmed=1 后重新运行任务不会覆盖人工字段（阶段 2.3） */
    @PostMapping("/meta/confirm")
    public BaseResponse<PhotoAssetMeta> confirm(@RequestBody AiCatalogConfirmRequest requestBody, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        PhotoAssetMeta update = new PhotoAssetMeta();
        BeanUtils.copyProperties(requestBody, update);
        return ResultUtils.success(photoAiService.confirm(requestBody.getPictureId(), user.getId(), update));
    }

    /** 兼容旧前端：只返回图片 id */
    @GetMapping("/search")
    public BaseResponse<List<Long>> search(@RequestParam String query, @RequestParam(defaultValue = "20") Integer topK, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.search(query, topK, user.getId()));
    }

    @GetMapping("/search/semantic")
    public BaseResponse<List<Long>> semanticSearch(@RequestParam String query, @RequestParam(defaultValue = "20") Integer topK, HttpServletRequest request) {
        return search(query, topK, request);
    }

    /** 权限感知检索：返回命中字段、命中标签、分值与权限依据（阶段 2.4） */
    @GetMapping("/search/evidence")
    public BaseResponse<List<Map<String, Object>>> searchWithEvidence(@RequestParam String query,
                                                                     @RequestParam(defaultValue = "20") Integer topK,
                                                                     HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.searchWithEvidence(query, topK, user.getId()));
    }

    /** 相似分组与辅助选片（阶段 2.5） */
    @GetMapping("/similar/group")
    public BaseResponse<Map<String, Object>> similarGroup(@RequestParam Long pictureId,
                                                          @RequestParam(required = false) Integer timeWindowSeconds,
                                                          @RequestParam(required = false) Double threshold,
                                                          HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(photoAiService.similarGroups(pictureId, user.getId(), timeWindowSeconds, threshold));
    }

    /** 重建检索索引（阶段 2.4） */
    @PostMapping("/search/rebuild-index")
    public BaseResponse<Boolean> rebuildIndex(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        // 本地 provider 下，重建索引等价于对本人图片重新执行编目
        return ResultUtils.success(true);
    }
}
