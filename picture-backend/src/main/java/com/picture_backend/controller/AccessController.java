package com.picture_backend.controller;

import com.picture_backend.model.entity.DownloadRecord;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.model.dto.access.DownloadAuthorizeRequest;
import com.picture_backend.model.dto.access.InviteCreateRequest;
import com.picture_backend.model.dto.access.InviteRedeemRequest;
import com.picture_backend.model.entity.DownloadQuota;
import com.picture_backend.model.entity.TeamInvite;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.AccessService;
import com.picture_backend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.util.StreamUtils;
import javax.servlet.http.HttpServletResponse;
import com.qcloud.cos.model.COSObject;

@RestController
@RequestMapping("/access")
public class AccessController {
    @Resource private AccessService accessService;
    @Resource private UserService userService;

    @PostMapping("/invite/create")
    public BaseResponse<Map<String, Object>> createInvite(@RequestBody InviteCreateRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(accessService.createInvite(body, user.getId())); }
    @PostMapping("/invite/redeem")
    public BaseResponse<Boolean> redeemInvite(@RequestBody InviteRedeemRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(accessService.redeemInvite(body.getCode(), user.getId())); }
    @GetMapping("/invite/list")
    public BaseResponse<List<TeamInvite>> listInvites(@RequestParam Long spaceId, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(accessService.listInvites(spaceId, user.getId())); }
    @PostMapping("/invite/revoke")
    public BaseResponse<Boolean> revokeInvite(@RequestParam Long inviteId, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(accessService.revokeInvite(inviteId, user.getId())); }
    @GetMapping("/quota/today")
    public BaseResponse<DownloadQuota> today(HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(accessService.getTodayQuota(user.getId())); }
    @PostMapping("/download/authorize")
    public BaseResponse<Map<String, Object>> authorize(@RequestBody DownloadAuthorizeRequest body, HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(accessService.authorizeDownload(body, user.getId())); }
    @GetMapping("/download/records")
    public BaseResponse<List<DownloadRecord>> records(HttpServletRequest request) { User user = userService.getLoginUser(request); return ResultUtils.success(accessService.listDownloadRecords(user.getId())); }

    @GetMapping("/download/file")
    public void downloadFile(@RequestParam String token, HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        User user = userService.getLoginUser(request);
        COSObject object = accessService.openDownload(token, user.getId());
        response.setContentType("application/octet-stream");
        try (java.io.InputStream inputStream = object.getObjectContent()) {
            StreamUtils.copy(inputStream, response.getOutputStream());
        }
    }
}
