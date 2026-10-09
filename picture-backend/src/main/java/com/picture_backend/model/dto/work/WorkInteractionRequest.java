package com.picture_backend.model.dto.work;

import lombok.Data;

import java.io.Serializable;

@Data
public class WorkInteractionRequest implements Serializable {
    private Long workId;
}
