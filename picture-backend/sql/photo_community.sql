-- 智能摄影云社区增量表
-- 在 smart_photo_community/create_table.sql 执行完成后执行本文件。
USE smart_photo_community;

CREATE TABLE IF NOT EXISTS photo_asset_process (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    taskId VARCHAR(64) NOT NULL,
    pictureId BIGINT NULL,
    userId BIGINT NOT NULL,
    spaceId BIGINT NULL,
    sourceType VARCHAR(16) NOT NULL,
    status VARCHAR(16) DEFAULT 'PENDING' NOT NULL,
    errorMessage VARCHAR(512) NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_asset_process_task_user (taskId, userId),
    INDEX idx_asset_process_picture (pictureId),
    INDEX idx_asset_process_user_time (userId, createTime)
) COMMENT '摄影资产上传处理状态';

CREATE TABLE IF NOT EXISTS photo_asset_meta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pictureId BIGINT NOT NULL,
    exifJson TEXT NULL,
    cameraMake VARCHAR(128) NULL,
    cameraModel VARCHAR(128) NULL,
    lensModel VARCHAR(128) NULL,
    shootTime DATETIME NULL,
    shootLocation VARCHAR(256) NULL,
    orientation VARCHAR(32) NULL,
    dominantColor VARCHAR(32) NULL,
    subjectTags VARCHAR(1024) NULL,
    sceneTags VARCHAR(1024) NULL,
    lightTags VARCHAR(1024) NULL,
    compositionTags VARCHAR(1024) NULL,
    qualityScore DECIMAL(5,2) NULL,
    confirmed TINYINT DEFAULT 0 NOT NULL,
    version INT DEFAULT 1 NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_photo_asset_picture (pictureId),
    INDEX idx_photo_asset_camera (cameraModel),
    INDEX idx_photo_asset_shootTime (shootTime)
) COMMENT '摄影资产元数据';

CREATE TABLE IF NOT EXISTS photo_ai_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pictureId BIGINT NULL,
    workId BIGINT NULL,
    taskType VARCHAR(32) NOT NULL,
    status VARCHAR(16) DEFAULT 'PENDING' NOT NULL,
    provider VARCHAR(64) DEFAULT 'LOCAL_FALLBACK' NOT NULL,
    providerVersion VARCHAR(64) NULL,
    requestId VARCHAR(128) NULL,
    retryCount INT DEFAULT 0 NOT NULL,
    resultJson TEXT NULL,
    errorMessage VARCHAR(1024) NULL,
    startedAt DATETIME NULL,
    finishedAt DATETIME NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_ai_task_picture (pictureId),
    INDEX idx_ai_task_status (status)
) COMMENT '摄影 AI 任务';

CREATE TABLE IF NOT EXISTS photo_vector (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pictureId BIGINT NOT NULL,
    content TEXT NULL,
    keywords VARCHAR(2048) NULL,
    modelVersion VARCHAR(64) DEFAULT 'keyword-v1' NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_photo_vector_picture (pictureId),
    FULLTEXT KEY ft_photo_vector_content (content, keywords)
) COMMENT '摄影检索特征';

CREATE TABLE IF NOT EXISTS photo_work (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    authorId BIGINT NOT NULL,
    spaceId BIGINT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(2000) NULL,
    shootTime DATETIME NULL,
    shootLocation VARCHAR(256) NULL,
    visibility VARCHAR(32) DEFAULT 'PUBLIC' NOT NULL,
    downloadPolicy VARCHAR(32) DEFAULT 'QUOTA_DOWNLOAD' NOT NULL,
    copyrightNotice VARCHAR(512) NULL,
    status VARCHAR(16) DEFAULT 'DRAFT' NOT NULL,
    viewCount BIGINT DEFAULT 0 NOT NULL,
    likeCount BIGINT DEFAULT 0 NOT NULL,
    commentCount BIGINT DEFAULT 0 NOT NULL,
    favoriteCount BIGINT DEFAULT 0 NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    INDEX idx_work_feed (status, createTime),
    INDEX idx_work_author (authorId, status, createTime)
) COMMENT '摄影作品';

SET @photo_work_space_id_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'photo_work' AND COLUMN_NAME = 'spaceId');
SET @photo_work_space_id_sql = IF(@photo_work_space_id_exists = 0, 'ALTER TABLE photo_work ADD COLUMN spaceId BIGINT NULL AFTER authorId', 'SELECT 1');
PREPARE photo_work_space_id_stmt FROM @photo_work_space_id_sql;
EXECUTE photo_work_space_id_stmt;
DEALLOCATE PREPARE photo_work_space_id_stmt;

CREATE TABLE IF NOT EXISTS photo_work_picture (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workId BIGINT NOT NULL,
    pictureId BIGINT NOT NULL,
    sortOrder INT DEFAULT 0 NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_work_picture (workId, pictureId),
    INDEX idx_work_picture_work (workId)
) COMMENT '作品图片关系';

CREATE TABLE IF NOT EXISTS work_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_work_like (workId, userId)
) COMMENT '作品点赞';

CREATE TABLE IF NOT EXISTS work_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_work_favorite (workId, userId),
    INDEX idx_work_favorite_user (userId)
) COMMENT '作品收藏';

CREATE TABLE IF NOT EXISTS work_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    parentId BIGINT NULL,
    content VARCHAR(1000) NOT NULL,
    status VARCHAR(16) DEFAULT 'VISIBLE' NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    INDEX idx_work_comment (workId, createTime)
) COMMENT '作品评论';

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    followerId BIGINT NOT NULL,
    followingId BIGINT NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_user_follow (followerId, followingId)
) COMMENT '用户关注';

CREATE TABLE IF NOT EXISTS team_invite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spaceId BIGINT NOT NULL,
    codeHash VARCHAR(128) NOT NULL,
    spaceRole VARCHAR(32) DEFAULT 'viewer' NOT NULL,
    maxUses INT DEFAULT 1 NOT NULL,
    usedCount INT DEFAULT 0 NOT NULL,
    expireTime DATETIME NULL,
    status VARCHAR(16) DEFAULT 'ACTIVE' NOT NULL,
    createUserId BIGINT NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_team_invite_hash (codeHash),
    INDEX idx_team_invite_space (spaceId, status)
) COMMENT '团队邀请码';

CREATE TABLE IF NOT EXISTS download_quota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    quotaDate DATE NOT NULL,
    quotaLimit INT DEFAULT 5 NOT NULL,
    usedCount INT DEFAULT 0 NOT NULL,
    ruleVersion VARCHAR(32) DEFAULT 'default-v1' NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_download_quota_user_date (userId, quotaDate)
) COMMENT '每日下载配额';

CREATE TABLE IF NOT EXISTS download_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requestId VARCHAR(128) NOT NULL,
    downloadToken VARCHAR(128) NULL,
    userId BIGINT NOT NULL,
    workId BIGINT NULL,
    pictureId BIGINT NOT NULL,
    spaceId BIGINT NULL,
    quotaDate DATE NOT NULL,
    quotaCost INT DEFAULT 1 NOT NULL,
    result VARCHAR(16) DEFAULT 'SUCCESS' NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_download_record_request (requestId),
    INDEX idx_download_record_user (userId, createTime),
    INDEX idx_download_record_picture (pictureId, createTime)
) COMMENT '下载审计';

SET @download_token_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'download_record' AND COLUMN_NAME = 'downloadToken');
SET @download_token_sql = IF(@download_token_exists = 0, 'ALTER TABLE download_record ADD COLUMN downloadToken VARCHAR(128) NULL AFTER requestId', 'SELECT 1');
PREPARE download_token_stmt FROM @download_token_sql;
EXECUTE download_token_stmt;
DEALLOCATE PREPARE download_token_stmt;

CREATE TABLE IF NOT EXISTS exhibition_work (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exhibitionId BIGINT NOT NULL,
    workId BIGINT NOT NULL,
    caption VARCHAR(512) NULL,
    sortOrder INT DEFAULT 0 NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_exhibition_work (exhibitionId, workId),
    INDEX idx_exhibition_work_order (exhibitionId, sortOrder)
) COMMENT '专题展览作品关系';

CREATE TABLE IF NOT EXISTS exhibition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    theme VARCHAR(256) NULL,
    coverUrl VARCHAR(1024) NULL,
    description VARCHAR(2048) NULL,
    status INT DEFAULT 0 NOT NULL,
    startTime DATETIME NULL,
    endTime DATETIME NULL,
    viewCount BIGINT DEFAULT 0 NOT NULL,
    userId BIGINT NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    editTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    INDEX idx_exhibition_status (status, createTime),
    INDEX idx_exhibition_user (userId)
) COMMENT '专题展览' collate = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exhibition_picture (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exhibitionId BIGINT NOT NULL,
    pictureId BIGINT NOT NULL,
    caption VARCHAR(512) NULL,
    sortOrder INT DEFAULT 0 NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    INDEX idx_exhibition_picture_order (exhibitionId, sortOrder)
) COMMENT '展览展品关联' collate = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS picture_collection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    editTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    INDEX idx_collection_user (userId)
) COMMENT '收藏夹' collate = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS collection_picture (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collectionId BIGINT NOT NULL,
    pictureId BIGINT NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    UNIQUE KEY uk_collection_picture (collectionId, pictureId),
    INDEX idx_collection_picture_picture (pictureId)
) COMMENT '收藏图片关联' collate = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS browse_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    pictureId BIGINT NOT NULL,
    browseTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_browse_history_user (userId, browseTime),
    INDEX idx_browse_history_picture (pictureId)
) COMMENT '浏览历史' collate = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    action VARCHAR(128) NULL,
    module VARCHAR(64) NULL,
    detail VARCHAR(1024) NULL,
    ip VARCHAR(64) NULL,
    costMs BIGINT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_operation_log_user (userId, createTime)
) COMMENT '操作日志' collate = utf8mb4_unicode_ci;
