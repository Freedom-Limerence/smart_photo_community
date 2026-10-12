package com.picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.picture_backend.model.vo.PictureVO;
import com.picture_backend.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("photo_work")
public class PhotoWork implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authorId;
    private Long spaceId;
    private String title;
    private String description;
    private Date shootTime;
    private String shootLocation;
    private String visibility;
    private String downloadPolicy;
    private String copyrightNotice;
    private String status;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long favoriteCount;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDelete;
    @TableField(exist = false)
    private String coverUrl;
    @TableField(exist = false)
    private String thumbnailUrl;
    @TableField(exist = false)
    private java.util.List<PictureVO> pictures;
    @TableField(exist = false)
    private UserVO author;
}
