package com.picture_backend.model.dto.exhibition;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ExhibitionWorkRequest implements Serializable {
    private Long exhibitionId;
    private List<Long> workIds;
    private String caption;
}
