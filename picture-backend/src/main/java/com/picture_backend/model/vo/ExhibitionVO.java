package com.picture_backend.model.vo;

import com.picture_backend.model.entity.Exhibition;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 专题展览视图
 */
@Data
public class ExhibitionVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 展览标题
     */
    private String title;

    /**
     * 展览主题
     */
    private String theme;

    /**
     * 封面图 url
     */
    private String coverUrl;

    /**
     * 展览介绍
     */
    private String description;

    /**
     * 状态：0-草稿 1-已发布 2-已下线
     */
    private Integer status;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 创建用户 id
     */
    private Long userId;

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

    /**
     * 展品数量（冗余统计字段）
     */
    private Long pictureCount;

    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     */
    public static Exhibition voToObj(ExhibitionVO exhibitionVO) {
        if (exhibitionVO == null) {
            return null;
        }
        Exhibition exhibition = new Exhibition();
        BeanUtils.copyProperties(exhibitionVO, exhibition);
        return exhibition;
    }

    /**
     * 对象转封装类
     */
    public static ExhibitionVO objToVo(Exhibition exhibition) {
        if (exhibition == null) {
            return null;
        }
        ExhibitionVO exhibitionVO = new ExhibitionVO();
        BeanUtils.copyProperties(exhibition, exhibitionVO);
        return exhibitionVO;
    }
}
