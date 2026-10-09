package com.picture_backend.model.dto.exhibition;

import com.picture_backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询展览请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExhibitionQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题（模糊搜索）
     */
    private String searchText;

    /**
     * 主题
     */
    private String theme;

    /**
     * 状态：0-草稿 1-已发布 2-已下线（为 null 时用户端默认只查已发布）
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
