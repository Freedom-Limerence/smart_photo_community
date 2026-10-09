package com.picture_backend.model.dto.exhibition;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 展览展品编排请求
 */
@Data
public class ExhibitionPictureRequest implements Serializable {

    /**
     * 展览 id
     */
    private Long exhibitionId;

    /**
     * 图片 id 列表（addPicture/removePicture/sortPictures 共用：
     * 新增/移除时可传多个，排序时按列表顺序写入 sortOrder）
     */
    private List<Long> pictureIds;

    /**
     * 展品说明（仅 addPicture 单张时有效）
     */
    private String caption;

    private static final long serialVersionUID = 1L;
}
