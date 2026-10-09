package com.picture_backend.model.vo;

import com.picture_backend.model.entity.PictureCollection;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 收藏夹视图
 */
@Data
public class PictureCollectionVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 所属用户 id
     */
    private Long userId;

    /**
     * 收藏夹名称
     */
    private String name;

    /**
     * 收藏夹描述
     */
    private String description;

    /**
     * 收藏图片数量（冗余统计字段）
     */
    private Long pictureCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转封装类
     */
    public static PictureCollectionVO objToVo(PictureCollection pictureCollection) {
        if (pictureCollection == null) {
            return null;
        }
        PictureCollectionVO pictureCollectionVO = new PictureCollectionVO();
        BeanUtils.copyProperties(pictureCollection, pictureCollectionVO);
        return pictureCollectionVO;
    }
}
