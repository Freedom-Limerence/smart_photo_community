-- 智能摄影云社区完整初始化脚本
CREATE DATABASE IF NOT EXISTS smart_photo_community
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

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

-- 用户表
CREATE TABLE IF NOT EXISTS `user`
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    userAccount   VARCHAR(256)                           NOT NULL COMMENT '账号',
    userPassword  VARCHAR(512)                           NOT NULL COMMENT '密码',
    userName      VARCHAR(256)                           NULL COMMENT '用户昵称',
    userAvatar    VARCHAR(1024)                          NULL COMMENT '用户头像',
    userProfile   VARCHAR(512)                           NULL COMMENT '用户简介',
    userRole      VARCHAR(256) DEFAULT 'user'            NOT NULL COMMENT '用户角色：user/admin',
    vipExpireTime DATETIME                               NULL COMMENT '会员过期时间',
    vipCode       VARCHAR(128)                           NULL COMMENT '会员兑换码',
    vipNumber     BIGINT                                 NULL COMMENT '会员编号',
    editTime      DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    createTime    DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      TINYINT      DEFAULT 0                 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
    ) COMMENT '用户' COLLATE = utf8mb4_unicode_ci;

-- 图片表
CREATE TABLE IF NOT EXISTS picture
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    url           VARCHAR(512)                       NOT NULL COMMENT '图片 url',
    name          VARCHAR(128)                       NOT NULL COMMENT '图片名称',
    introduction  VARCHAR(512)                       NULL COMMENT '简介',
    category      VARCHAR(64)                        NULL COMMENT '分类',
    tags          VARCHAR(512)                       NULL COMMENT '标签（JSON 数组）',
    picSize       BIGINT                             NULL COMMENT '图片体积',
    picWidth      INT                                NULL COMMENT '图片宽度',
    picHeight     INT                                NULL COMMENT '图片高度',
    picScale      DOUBLE                             NULL COMMENT '图片宽高比例',
    picFormat     VARCHAR(32)                        NULL COMMENT '图片格式',
    userId        BIGINT                             NOT NULL COMMENT '创建用户 id',
    reviewStatus  INT      DEFAULT 0                 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage VARCHAR(512)                       NULL COMMENT '审核信息',
    reviewerId    BIGINT                             NULL COMMENT '审核人 ID',
    reviewTime    DATETIME                           NULL COMMENT '审核时间',
    thumbnailUrl  VARCHAR(512)                       NULL COMMENT '缩略图 url',
    spaceId       BIGINT                             NULL COMMENT '空间 id（为空表示公共空间）',
    picColor      VARCHAR(16)                        NULL COMMENT '图片主色调',
    createTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    INDEX idx_name (name),
    INDEX idx_introduction (introduction),
    INDEX idx_category (category),
    INDEX idx_tags (tags),
    INDEX idx_userId (userId),
    INDEX idx_reviewStatus (reviewStatus),
    INDEX idx_spaceId (spaceId)
    ) COMMENT '图片' COLLATE = utf8mb4_unicode_ci;

-- 空间表
CREATE TABLE IF NOT EXISTS space
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    spaceName  VARCHAR(128)                       NULL COMMENT '空间名称',
    spaceLevel INT      DEFAULT 0                 NULL COMMENT '空间级别：0-普通版 1-专业版 2-旗舰版',
    spaceType  INT      DEFAULT 0                 NOT NULL COMMENT '空间类型：0-私有 1-团队',
    maxSize    BIGINT   DEFAULT 0                 NULL COMMENT '空间图片的最大总大小',
    maxCount   BIGINT   DEFAULT 0                 NULL COMMENT '空间图片的最大数量',
    totalSize  BIGINT   DEFAULT 0                 NULL COMMENT '当前空间下图片的总大小',
    totalCount BIGINT   DEFAULT 0                 NULL COMMENT '当前空间下的图片数量',
    userId     BIGINT                             NOT NULL COMMENT '创建用户 id',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    INDEX idx_userId (userId),
    INDEX idx_spaceName (spaceName),
    INDEX idx_spaceLevel (spaceLevel),
    INDEX idx_spaceType (spaceType)
    ) COMMENT '空间' COLLATE = utf8mb4_unicode_ci;

-- 空间成员表
CREATE TABLE IF NOT EXISTS space_user
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    spaceId    BIGINT                                 NOT NULL COMMENT '空间 id',
    userId     BIGINT                                 NOT NULL COMMENT '用户 id',
    spaceRole  VARCHAR(128) DEFAULT 'viewer'          NULL COMMENT '空间角色：viewer/editor/admin',
    createTime DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_spaceId_userId (spaceId, userId),
    INDEX idx_spaceId (spaceId),
    INDEX idx_userId (userId)
    ) COMMENT '空间用户关联' COLLATE = utf8mb4_unicode_ci;

-- 摄影资产元数据
CREATE TABLE IF NOT EXISTS photo_asset_meta
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pictureId       BIGINT                             NOT NULL,
    exifJson        TEXT                               NULL,
    cameraMake      VARCHAR(128)                       NULL,
    cameraModel     VARCHAR(128)                       NULL,
    lensModel       VARCHAR(128)                       NULL,
    shootTime       DATETIME                           NULL,
    shootLocation   VARCHAR(256)                       NULL,
    orientation     VARCHAR(32)                        NULL,
    dominantColor   VARCHAR(32)                        NULL,
    subjectTags     VARCHAR(1024)                      NULL,
    sceneTags       VARCHAR(1024)                      NULL,
    lightTags       VARCHAR(1024)                      NULL,
    compositionTags VARCHAR(1024)                      NULL,
    qualityScore    DECIMAL(5, 2)                      NULL,
    confirmed       TINYINT  DEFAULT 0                 NOT NULL,
    version         INT      DEFAULT 1                 NOT NULL,
    createTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_photo_asset_picture (pictureId),
    INDEX idx_photo_asset_camera (cameraModel),
    INDEX idx_photo_asset_shootTime (shootTime)
    ) COMMENT '摄影资产元数据' COLLATE = utf8mb4_unicode_ci;

-- 摄影 AI 任务
CREATE TABLE IF NOT EXISTS photo_ai_task
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pictureId       BIGINT                             NULL,
    workId          BIGINT                             NULL,
    taskType        VARCHAR(32)                        NOT NULL,
    status          VARCHAR(16) DEFAULT 'PENDING'      NOT NULL,
    provider        VARCHAR(64) DEFAULT 'LOCAL_FALLBACK' NOT NULL,
    providerVersion VARCHAR(64)                        NULL,
    requestId       VARCHAR(128)                       NULL,
    retryCount      INT         DEFAULT 0              NOT NULL,
    resultJson      TEXT                               NULL,
    errorMessage    VARCHAR(1024)                      NULL,
    startedAt       DATETIME                           NULL,
    finishedAt      DATETIME                           NULL,
    createTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_task_picture (pictureId),
    INDEX idx_ai_task_status (status)
    ) COMMENT '摄影 AI 任务' COLLATE = utf8mb4_unicode_ci;

-- 摄影检索特征
CREATE TABLE IF NOT EXISTS photo_vector
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    pictureId    BIGINT                             NOT NULL,
    content      TEXT                               NULL,
    keywords     VARCHAR(2048)                      NULL,
    modelVersion VARCHAR(64) DEFAULT 'keyword-v1'   NOT NULL,
    createTime   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_photo_vector_picture (pictureId),
    FULLTEXT KEY ft_photo_vector_content (content, keywords)
    ) COMMENT '摄影检索特征' COLLATE = utf8mb4_unicode_ci;

-- 摄影作品
CREATE TABLE IF NOT EXISTS photo_work
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    authorId        BIGINT                             NOT NULL,
    spaceId         BIGINT                             NULL,
    title           VARCHAR(128)                       NOT NULL,
    description     VARCHAR(2000)                      NULL,
    shootTime       DATETIME                           NULL,
    shootLocation   VARCHAR(256)                       NULL,
    visibility      VARCHAR(32) DEFAULT 'PUBLIC'       NOT NULL,
    downloadPolicy  VARCHAR(32) DEFAULT 'QUOTA_DOWNLOAD' NOT NULL,
    copyrightNotice VARCHAR(512)                       NULL,
    status          VARCHAR(16) DEFAULT 'DRAFT'        NOT NULL,
    viewCount       BIGINT      DEFAULT 0              NOT NULL,
    likeCount       BIGINT      DEFAULT 0              NOT NULL,
    commentCount    BIGINT      DEFAULT 0              NOT NULL,
    favoriteCount   BIGINT      DEFAULT 0              NOT NULL,
    createTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete        TINYINT     DEFAULT 0              NOT NULL,
    INDEX idx_work_feed (status, createTime),
    INDEX idx_work_author (authorId, status, createTime)
    ) COMMENT '摄影作品' COLLATE = utf8mb4_unicode_ci;

-- 作品图片关系
CREATE TABLE IF NOT EXISTS photo_work_picture
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    workId     BIGINT                             NOT NULL,
    pictureId  BIGINT                             NOT NULL,
    sortOrder  INT      DEFAULT 0                 NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_work_picture (workId, pictureId),
    INDEX idx_work_picture_work (workId)
    ) COMMENT '作品图片关系' COLLATE = utf8mb4_unicode_ci;

-- 作品点赞
CREATE TABLE IF NOT EXISTS work_like
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    workId     BIGINT                             NOT NULL,
    userId     BIGINT                             NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_work_like (workId, userId)
    ) COMMENT '作品点赞' COLLATE = utf8mb4_unicode_ci;

-- 作品收藏
CREATE TABLE IF NOT EXISTS work_favorite
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    workId     BIGINT                             NOT NULL,
    userId     BIGINT                             NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_work_favorite (workId, userId),
    INDEX idx_work_favorite_user (userId)
    ) COMMENT '作品收藏' COLLATE = utf8mb4_unicode_ci;

-- 作品评论
CREATE TABLE IF NOT EXISTS work_comment
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    workId     BIGINT                             NOT NULL,
    userId     BIGINT                             NOT NULL,
    parentId   BIGINT                             NULL,
    content    VARCHAR(1000)                      NOT NULL,
    status     VARCHAR(16) DEFAULT 'VISIBLE'      NOT NULL,
    createTime DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete   TINYINT     DEFAULT 0              NOT NULL,
    INDEX idx_work_comment (workId, createTime)
    ) COMMENT '作品评论' COLLATE = utf8mb4_unicode_ci;

-- 用户关注
CREATE TABLE IF NOT EXISTS user_follow
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    followerId  BIGINT                             NOT NULL,
    followingId BIGINT                             NOT NULL,
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_user_follow (followerId, followingId)
    ) COMMENT '用户关注' COLLATE = utf8mb4_unicode_ci;

-- 团队邀请码
CREATE TABLE IF NOT EXISTS team_invite
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    spaceId      BIGINT                             NOT NULL,
    codeHash     VARCHAR(128)                       NOT NULL,
    spaceRole    VARCHAR(32) DEFAULT 'viewer'       NOT NULL,
    maxUses      INT         DEFAULT 1              NOT NULL,
    usedCount    INT         DEFAULT 0              NOT NULL,
    expireTime   DATETIME                           NULL,
    status       VARCHAR(16) DEFAULT 'ACTIVE'       NOT NULL,
    createUserId BIGINT                             NOT NULL,
    createTime   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_invite_hash (codeHash),
    INDEX idx_team_invite_space (spaceId, status)
    ) COMMENT '团队邀请码' COLLATE = utf8mb4_unicode_ci;

-- 每日下载配额
CREATE TABLE IF NOT EXISTS download_quota
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId      BIGINT                             NOT NULL,
    quotaDate   DATE                               NOT NULL,
    quotaLimit  INT      DEFAULT 5                 NOT NULL,
    usedCount   INT      DEFAULT 0                 NOT NULL,
    ruleVersion VARCHAR(32) DEFAULT 'default-v1'   NOT NULL,
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_download_quota_user_date (userId, quotaDate)
    ) COMMENT '每日下载配额' COLLATE = utf8mb4_unicode_ci;

-- 下载审计
CREATE TABLE IF NOT EXISTS download_record
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    requestId     VARCHAR(128)                       NOT NULL,
    downloadToken VARCHAR(128)                       NULL,
    userId        BIGINT                             NOT NULL,
    workId        BIGINT                             NULL,
    pictureId     BIGINT                             NOT NULL,
    spaceId       BIGINT                             NULL,
    quotaDate     DATE                               NOT NULL,
    quotaCost     INT      DEFAULT 1                 NOT NULL,
    result        VARCHAR(16) DEFAULT 'SUCCESS'      NOT NULL,
    createTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_download_record_request (requestId),
    INDEX idx_download_record_user (userId, createTime),
    INDEX idx_download_record_picture (pictureId, createTime)
    ) COMMENT '下载审计' COLLATE = utf8mb4_unicode_ci;

-- 专题展览
CREATE TABLE IF NOT EXISTS exhibition
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(256)                       NOT NULL,
    theme       VARCHAR(256)                       NULL,
    coverUrl    VARCHAR(1024)                      NULL,
    description VARCHAR(2048)                      NULL,
    status      INT      DEFAULT 0                 NOT NULL,
    startTime   DATETIME                           NULL,
    endTime     DATETIME                           NULL,
    viewCount   BIGINT   DEFAULT 0                 NOT NULL,
    userId      BIGINT                             NOT NULL,
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    editTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete    TINYINT  DEFAULT 0                 NOT NULL,
    INDEX idx_exhibition_status (status, createTime),
    INDEX idx_exhibition_user (userId)
    ) COMMENT '专题展览' COLLATE = utf8mb4_unicode_ci;

-- 专题展览作品关系
CREATE TABLE IF NOT EXISTS exhibition_work
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    exhibitionId BIGINT                             NOT NULL,
    workId       BIGINT                             NOT NULL,
    caption      VARCHAR(512)                       NULL,
    sortOrder    INT      DEFAULT 0                 NOT NULL,
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exhibition_work (exhibitionId, workId),
    INDEX idx_exhibition_work_order (exhibitionId, sortOrder)
    ) COMMENT '专题展览作品关系' COLLATE = utf8mb4_unicode_ci;

-- 展览展品关联
CREATE TABLE IF NOT EXISTS exhibition_picture
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    exhibitionId BIGINT                             NOT NULL,
    pictureId    BIGINT                             NOT NULL,
    caption      VARCHAR(512)                       NULL,
    sortOrder    INT      DEFAULT 0                 NOT NULL,
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete     TINYINT  DEFAULT 0                 NOT NULL,
    INDEX idx_exhibition_picture_order (exhibitionId, sortOrder)
    ) COMMENT '展览展品关联' COLLATE = utf8mb4_unicode_ci;

-- 收藏夹
CREATE TABLE IF NOT EXISTS picture_collection
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId      BIGINT                             NOT NULL,
    name        VARCHAR(128)                       NOT NULL,
    description VARCHAR(512)                       NULL,
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    editTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete    TINYINT  DEFAULT 0                 NOT NULL,
    INDEX idx_collection_user (userId)
    ) COMMENT '收藏夹' COLLATE = utf8mb4_unicode_ci;

-- 收藏图片关联
CREATE TABLE IF NOT EXISTS collection_picture
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    collectionId BIGINT                             NOT NULL,
    pictureId    BIGINT                             NOT NULL,
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete     TINYINT  DEFAULT 0                 NOT NULL,
    UNIQUE KEY uk_collection_picture (collectionId, pictureId),
    INDEX idx_collection_picture_picture (pictureId)
    ) COMMENT '收藏图片关联' COLLATE = utf8mb4_unicode_ci;

-- 浏览历史
CREATE TABLE IF NOT EXISTS browse_history
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId     BIGINT                             NOT NULL,
    pictureId  BIGINT                             NOT NULL,
    browseTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_browse_history_user (userId, browseTime),
    INDEX idx_browse_history_picture (pictureId)
    ) COMMENT '浏览历史' COLLATE = utf8mb4_unicode_ci;

-- 操作日志
CREATE TABLE IF NOT EXISTS operation_log
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId     BIGINT                             NOT NULL,
    `action`   VARCHAR(128)                       NULL,
    module     VARCHAR(64)                        NULL,
    detail     VARCHAR(1024)                      NULL,
    ip         VARCHAR(64)                        NULL,
    costMs     BIGINT                             NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_operation_log_user (userId, createTime)
    ) COMMENT '操作日志' COLLATE = utf8mb4_unicode_ci;
