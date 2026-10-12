-- 智能摄影云社区平台首次初始化 SQL
-- 数据库：smart_photo_community
-- 本文件已按依赖顺序合并：
-- 1. 原云图库基础表
-- 2. 摄影社区增量表
--
-- 推荐执行：
-- mysql -u root -p < init_smart_photo_community.sql
--
-- 本脚本适用于首次初始化空环境。原始拆分脚本仍保留在同一目录，
-- 如需排查或分步升级，可分别执行 create_table.sql 和 photo_community.sql。

-- ===== create_table.sql：原云图库基础表 =====

-- 创建库
create database if not exists smart_photo_community;

-- 切换库
use smart_photo_community;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;


ALTER TABLE picture
    -- 添加新列
    ADD COLUMN reviewStatus INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512) NULL COMMENT '审核信息',
    ADD COLUMN reviewerId BIGINT NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime DATETIME NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN thumbnailUrl varchar(512) NULL COMMENT '缩略图 url';


-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;

-- 添加新列
ALTER TABLE picture
    ADD COLUMN spaceId bigint  null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (spaceId);

-- 添加新列
ALTER TABLE picture
    ADD COLUMN picColor varchar(16) null comment '图片主色调';

-- 支持空间类型，添加新列
ALTER TABLE space
    ADD COLUMN spaceType int default 0 not null comment '空间类型：0-私有 1-团队';

CREATE INDEX idx_spaceType ON space (spaceType);

-- 空间成员表
create table if not exists space_user
(
    id         bigint auto_increment comment 'id' primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_spaceId_userId (spaceId, userId), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_spaceId (spaceId),                    -- 提升按空间查询的性能
    INDEX idx_userId (userId)                       -- 提升按用户查询的性能
) comment '空间用户关联' collate = utf8mb4_unicode_ci;

-- 扩展用户表：新增会员功能
ALTER TABLE user
    ADD COLUMN vipExpireTime datetime NULL COMMENT '会员过期时间',
    ADD COLUMN vipCode varchar(128) NULL COMMENT '会员兑换码',
    ADD COLUMN vipNumber bigint NULL COMMENT '会员编号';



-- ===== photo_community.sql：摄影社区增量表 =====

-- 智能摄影云社区增量表
-- 在 smart_photo_community/create_table.sql 执行完成后执行本文件。
USE smart_photo_community;

-- 图片上传及后处理状态。可重复执行，保留原 picture 表不变。
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


