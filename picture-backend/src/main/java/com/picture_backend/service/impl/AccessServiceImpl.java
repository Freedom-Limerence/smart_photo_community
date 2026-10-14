package com.picture_backend.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.DownloadQuotaMapper;
import com.picture_backend.mapper.DownloadRecordMapper;
import com.picture_backend.mapper.SpaceUserMapper;
import com.picture_backend.mapper.TeamInviteMapper;
import com.picture_backend.model.entity.*;
import com.picture_backend.mapper.*;
import com.picture_backend.model.dto.access.DownloadAuthorizeRequest;
import com.picture_backend.model.dto.access.InviteCreateRequest;
import com.picture_backend.model.entity.*;
import com.picture_backend.model.enums.SpaceRoleEnum;
import com.picture_backend.service.AccessService;
import com.picture_backend.service.PictureService;
import com.picture_backend.service.SpaceService;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.manager.CosManager;
import com.picture_backend.config.CosClientConfig;
import com.qcloud.cos.model.COSObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import jakarta.annotation.Resource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@Service
public class AccessServiceImpl extends ServiceImpl<DownloadQuotaMapper, DownloadQuota> implements AccessService {
    @Resource private TeamInviteMapper teamInviteMapper;
    @Resource private DownloadRecordMapper downloadRecordMapper;
    @Resource private SpaceUserMapper spaceUserMapper;
    @Resource private SpaceService spaceService;
    @Resource private PhotoWorkService photoWorkService;
    @Resource private PhotoWorkPictureMapper photoWorkPictureMapper;
    @Resource private PictureService pictureService;
    @Resource private CosManager cosManager;
    @Resource private CosClientConfig cosClientConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createInvite(InviteCreateRequest request, Long userId) {
        if (request == null || request.getSpaceId() == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String requestedRole = request.getSpaceRole();
        if (requestedRole != null) requestedRole = requestedRole.trim();
        if (requestedRole != null && !requestedRole.isEmpty()
                && (!SpaceRoleEnum.VIEWER.getValue().equals(requestedRole)
                && !SpaceRoleEnum.EDITOR.getValue().equals(requestedRole))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请码角色只能是 viewer 或 editor");
        }
        Space space = spaceService.getById(request.getSpaceId());
        if (space == null || !canManageSpace(space, userId)) throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有团队邀请码管理权限");
        String code = RandomUtil.randomStringUpper(10);
        TeamInvite invite = new TeamInvite();
        invite.setSpaceId(space.getId());
        invite.setCodeHash(DigestUtil.sha256Hex(code));
        invite.setSpaceRole(requestedRole == null || requestedRole.isEmpty()
                ? SpaceRoleEnum.VIEWER.getValue() : requestedRole.trim());
        invite.setMaxUses(request.getMaxUses() == null || request.getMaxUses() < 1 ? 1 : Math.min(request.getMaxUses(), 100));
        invite.setUsedCount(0);
        invite.setExpireTime(request.getExpireTime());
        invite.setStatus("ACTIVE");
        invite.setCreateUserId(userId);
        teamInviteMapper.insert(invite);
        Map<String, Object> result = new HashMap<>();
        result.put("id", invite.getId());
        result.put("code", code);
        result.put("spaceId", invite.getSpaceId());
        result.put("expireTime", invite.getExpireTime());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean redeemInvite(String code, Long userId) {
        if (code == null || code.trim().isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请码不能为空");
        TeamInvite invite = teamInviteMapper.selectOne(new LambdaQueryWrapper<TeamInvite>().eq(TeamInvite::getCodeHash, DigestUtil.sha256Hex(code.trim().toUpperCase())));
        if (invite == null || !"ACTIVE".equals(invite.getStatus()) || (invite.getExpireTime() != null && invite.getExpireTime().before(new java.util.Date())) || invite.getUsedCount() >= invite.getMaxUses()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "邀请码无效或已用尽");
        }
        SpaceUser existing = spaceUserMapper.selectOne(new LambdaQueryWrapper<SpaceUser>().eq(SpaceUser::getSpaceId, invite.getSpaceId()).eq(SpaceUser::getUserId, userId));
        if (existing != null) return true;
        SpaceUser member = new SpaceUser();
        member.setSpaceId(invite.getSpaceId());
        member.setUserId(userId);
        member.setSpaceRole(invite.getSpaceRole());
        try {
            // 先写入唯一成员关系，重复兑换直接由唯一索引保证幂等，不消耗邀请码次数。
            spaceUserMapper.insert(member);
        } catch (DuplicateKeyException e) {
            if (spaceUserMapper.selectOne(new LambdaQueryWrapper<SpaceUser>()
                    .eq(SpaceUser::getSpaceId, invite.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)) != null) return true;
            throw e;
        }
        int changed = teamInviteMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TeamInvite>()
                .eq(TeamInvite::getId, invite.getId())
                .eq(TeamInvite::getStatus, "ACTIVE")
                .lt(TeamInvite::getUsedCount, invite.getMaxUses())
                .setSql("usedCount = usedCount + 1"));
        if (changed != 1) throw new BusinessException(ErrorCode.OPERATION_ERROR, "邀请码已被使用");
        return true;
    }

    @Override
    public List<TeamInvite> listInvites(Long spaceId, Long userId) {
        Space space = spaceService.getById(spaceId);
        if (space == null || !canManageSpace(space, userId)) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        return teamInviteMapper.selectList(new LambdaQueryWrapper<TeamInvite>().eq(TeamInvite::getSpaceId, spaceId).orderByDesc(TeamInvite::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeInvite(Long inviteId, Long userId) {
        TeamInvite invite = teamInviteMapper.selectById(inviteId);
        if (invite == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        Space space = spaceService.getById(invite.getSpaceId());
        if (space == null || !canManageSpace(space, userId)) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        return teamInviteMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TeamInvite>()
                .eq(TeamInvite::getId, inviteId).set(TeamInvite::getStatus, "REVOKED")) > 0;
    }

    private boolean canManageSpace(Space space, Long userId) {
        if (userId.equals(space.getUserId())) return true;
        SpaceUser member = spaceUserMapper.selectOne(new LambdaQueryWrapper<SpaceUser>()
                .eq(SpaceUser::getSpaceId, space.getId())
                .eq(SpaceUser::getUserId, userId));
        return member != null && SpaceRoleEnum.ADMIN.getValue().equals(member.getSpaceRole());
    }

    @Override
    public DownloadQuota getTodayQuota(Long userId) {
        Date today = Date.valueOf(LocalDate.now());
        DownloadQuota quota = baseMapper.selectOne(new LambdaQueryWrapper<DownloadQuota>().eq(DownloadQuota::getUserId, userId).eq(DownloadQuota::getQuotaDate, today));
        if (quota != null) return quota;
        quota = new DownloadQuota();
        quota.setUserId(userId);
        quota.setQuotaDate(today);
        quota.setQuotaLimit(5);
        quota.setUsedCount(0);
        quota.setRuleVersion("default-v1");
        try { baseMapper.insert(quota); } catch (Exception ignored) { }
        return baseMapper.selectOne(new LambdaQueryWrapper<DownloadQuota>().eq(DownloadQuota::getUserId, userId).eq(DownloadQuota::getQuotaDate, today));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> authorizeDownload(DownloadAuthorizeRequest request, Long userId) {
        if (request == null || request.getPictureId() == null || request.getRequestId() == null
                || request.getRequestId().trim().isEmpty() || request.getRequestId().trim().length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求标识不能为空且长度不能超过 128 个字符");
        }
        String requestId = request.getRequestId().trim();
        DownloadRecord old = downloadRecordMapper.selectOne(new LambdaQueryWrapper<DownloadRecord>().eq(DownloadRecord::getRequestId, requestId));
        if (old != null) {
            if (!userId.equals(old.getUserId())) throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求标识已被使用");
            if (!Objects.equals(old.getPictureId(), request.getPictureId()) || !Objects.equals(old.getWorkId(), request.getWorkId())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求标识已绑定其他下载目标");
            }
            return downloadResult(old);
        }
        Picture picture = pictureService.getById(request.getPictureId());
        if (picture == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        PhotoWork work = null;
        if (request.getWorkId() != null) {
            work = photoWorkService.getById(request.getWorkId());
            if (work == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作品不存在");
            boolean related = photoWorkPictureMapper.selectCount(new LambdaQueryWrapper<PhotoWorkPicture>()
                    .eq(PhotoWorkPicture::getWorkId, work.getId())
                    .eq(PhotoWorkPicture::getPictureId, picture.getId())) > 0;
            if (!related) throw new BusinessException(ErrorCode.PARAMS_ERROR, "作品与图片不匹配");
            photoWorkService.validateVisible(work.getId(), userId);
            validateWorkDownloadPolicy(work, userId);
        } else {
            ensureRestrictedWorkHasContext(picture.getId(), userId);
        }
        boolean owner = userId.equals(picture.getUserId());
        boolean teamAsset = picture.getSpaceId() != null;
        if (!owner && !teamAsset && (picture.getReviewStatus() == null || picture.getReviewStatus() != 1)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "图片尚未通过公开审核");
        }
        if (teamAsset) {
            SpaceUser member = spaceUserMapper.selectOne(new LambdaQueryWrapper<SpaceUser>()
                    .eq(SpaceUser::getSpaceId, picture.getSpaceId())
                    .eq(SpaceUser::getUserId, userId));
            if (member == null) throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有团队访问权限");
            if (SpaceRoleEnum.VIEWER.getValue().equals(member.getSpaceRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "浏览者不能下载团队原图");
            }
            if (!SpaceRoleEnum.EDITOR.getValue().equals(member.getSpaceRole())
                    && !SpaceRoleEnum.ADMIN.getValue().equals(member.getSpaceRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "空间角色无效");
            }
        }
        boolean consumeQuota = !teamAsset && !owner && (work == null
                || "QUOTA_DOWNLOAD".equals(work.getDownloadPolicy()));
        if (consumeQuota) {
            DownloadQuota quota = getTodayQuota(userId);
            int changed = baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DownloadQuota>().eq(DownloadQuota::getId, quota.getId()).lt(DownloadQuota::getUsedCount, quota.getQuotaLimit()).setSql("usedCount = usedCount + 1"));
            if (changed != 1) throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日下载配额已用尽");
        }
        DownloadRecord record = new DownloadRecord();
        record.setRequestId(requestId); record.setDownloadToken(UUID.randomUUID().toString().replace("-", "")); record.setUserId(userId); record.setWorkId(request.getWorkId()); record.setPictureId(picture.getId()); record.setSpaceId(picture.getSpaceId()); record.setQuotaDate(Date.valueOf(LocalDate.now())); record.setQuotaCost(consumeQuota ? 1 : 0); record.setResult("SUCCESS");
        downloadRecordMapper.insert(record);
        return downloadResult(record);
    }

    private void validateWorkDownloadPolicy(PhotoWork work, Long userId) {
        if (userId.equals(work.getAuthorId())) return;
        if ("OWNER_ONLY".equals(work.getDownloadPolicy())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该作品仅作者可下载");
        }
        if ("TEAM_ONLY".equals(work.getDownloadPolicy())) {
            if (work.getSpaceId() == null || spaceUserMapper.selectCount(new LambdaQueryWrapper<SpaceUser>()
                    .eq(SpaceUser::getSpaceId, work.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)) == 0) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有作品团队下载权限");
            }
        }
    }

    /**
     * A picture detail URL does not carry the work context. Do not let it bypass a
     * published work's visibility or download policy; require the caller to use
     * the work entry point so the complete policy can be checked.
     */
    private void ensureRestrictedWorkHasContext(Long pictureId, Long userId) {
        List<PhotoWorkPicture> relations = photoWorkPictureMapper.selectList(new LambdaQueryWrapper<PhotoWorkPicture>()
                .eq(PhotoWorkPicture::getPictureId, pictureId));
        for (PhotoWorkPicture relation : relations) {
            PhotoWork work = photoWorkService.getById(relation.getWorkId());
            if (work == null || !"PUBLISHED".equals(work.getStatus()) || userId.equals(work.getAuthorId())) continue;
            boolean restrictedVisibility = !"PUBLIC".equals(work.getVisibility());
            boolean restrictedDownload = !"QUOTA_DOWNLOAD".equals(work.getDownloadPolicy());
            if (restrictedVisibility || restrictedDownload) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请从作品详情进入下载，以校验作品访问策略");
            }
        }
    }

    @Override
    public List<DownloadRecord> listDownloadRecords(Long userId) {
        return downloadRecordMapper.selectList(new LambdaQueryWrapper<DownloadRecord>()
                .eq(DownloadRecord::getUserId, userId).orderByDesc(DownloadRecord::getCreateTime).last("limit 100"));
    }

    @Override
    public COSObject openDownload(String token, Long userId) {
        DownloadRecord record = downloadRecordMapper.selectOne(new LambdaQueryWrapper<DownloadRecord>()
                .eq(DownloadRecord::getDownloadToken, token).eq(DownloadRecord::getUserId, userId).eq(DownloadRecord::getResult, "SUCCESS"));
        if (record == null) throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "下载令牌无效");
        if (record.getCreateTime() == null || record.getCreateTime().before(new java.util.Date(System.currentTimeMillis() - 5 * 60 * 1000L))) throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载令牌已过期");
        Picture picture = pictureService.getById(record.getPictureId());
        if (picture == null || picture.getUrl() == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "原图不存在");
        String key = picture.getUrl();
        if (cosClientConfig.getHost() != null && key.startsWith(cosClientConfig.getHost())) key = key.substring(cosClientConfig.getHost().length());
        while (key.startsWith("/")) key = key.substring(1);
        return cosManager.getObject(key);
    }

    private Map<String, Object> downloadResult(DownloadRecord record) {
        Map<String, Object> result = new HashMap<>(); result.put("recordId", record.getId()); result.put("requestId", record.getRequestId()); result.put("pictureId", record.getPictureId()); result.put("downloadToken", record.getDownloadToken()); result.put("expiresInSeconds", 300); result.put("downloadPath", "/api/access/download/file?token=" + record.getDownloadToken()); result.put("quotaCost", record.getQuotaCost()); return result;
    }
}
