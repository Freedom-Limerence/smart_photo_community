package com.picture_backend.model.vo.work;

import lombok.Data;

import java.io.Serializable;

/** Public author fields shown beside a work comment. */
@Data
public class WorkCommentUserVO implements Serializable {
    private Long id;
    private String userName;
    private String userAvatar;
}
