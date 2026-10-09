package com.picture_backend.service;

import com.picture_backend.model.dto.access.DownloadAuthorizeRequest;
import com.picture_backend.model.dto.access.InviteCreateRequest;
import com.picture_backend.model.entity.DownloadRecord;
import com.picture_backend.model.entity.DownloadQuota;
import com.picture_backend.model.entity.TeamInvite;

import java.util.List;
import java.util.Map;
import com.qcloud.cos.model.COSObject;

public interface AccessService {
    Map<String, Object> createInvite(InviteCreateRequest request, Long userId);
    boolean redeemInvite(String code, Long userId);
    List<TeamInvite> listInvites(Long spaceId, Long userId);
    boolean revokeInvite(Long inviteId, Long userId);
    DownloadQuota getTodayQuota(Long userId);
    Map<String, Object> authorizeDownload(DownloadAuthorizeRequest request, Long userId);
    List<DownloadRecord> listDownloadRecords(Long userId);
    COSObject openDownload(String token, Long userId);
}
