package com.picture_backend.model.dto.work;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PhotoWorkRequest implements Serializable {
    private Long id;
    private Long spaceId;
    private String title;
    private String description;
    private Date shootTime;
    private String shootLocation;
    private String visibility;
    private String downloadPolicy;
    private String copyrightNotice;
    private List<Long> pictureIds;
}
