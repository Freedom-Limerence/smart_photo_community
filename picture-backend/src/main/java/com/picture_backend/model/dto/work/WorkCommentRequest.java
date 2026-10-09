package com.picture_backend.model.dto.work;

import lombok.Data;

import java.io.Serializable;

@Data
public class WorkCommentRequest implements Serializable {
    private Long workId;
    private Long parentId;
    private String content;
}
