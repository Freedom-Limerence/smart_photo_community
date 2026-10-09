package com.picture_backend.model.dto.ai;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiCatalogConfirmRequest implements Serializable {
    private Long pictureId;
    private String subjectTags;
    private String sceneTags;
    private String lightTags;
    private String compositionTags;
    private String shootLocation;
}
