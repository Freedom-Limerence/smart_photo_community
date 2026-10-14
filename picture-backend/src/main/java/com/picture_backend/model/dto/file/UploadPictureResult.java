package com.picture_backend.model.dto.file;

import lombok.Data;

/**
 * 上传图片的结果
 */
@Data
public class UploadPictureResult {

    /**
     * 图片地址
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private int picWidth;

    /**
     * 图片高度
     */
    private int picHeight;

    /**
     * 图片宽高比
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * EXIF 原始 JSON（在本地临时文件阶段解析，COS 压缩后会丢失）
     */
    private String exifJson;

    /**
     * 相机品牌
     */
    private String cameraMake;

    /**
     * 相机型号
     */
    private String cameraModel;

    /**
     * 镜头型号
     */
    private String lensModel;

    /**
     * 拍摄时间
     */
    private java.util.Date shootTime;

    /**
     * 拍摄方向（LANDSCAPE / PORTRAIT）
     */
    private String orientation;
}