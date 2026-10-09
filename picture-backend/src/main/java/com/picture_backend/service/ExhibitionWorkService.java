package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.picture_backend.model.dto.exhibition.ExhibitionWorkRequest;
import com.picture_backend.model.entity.ExhibitionWork;

import java.util.List;

public interface ExhibitionWorkService extends IService<ExhibitionWork> {
    void addWorks(ExhibitionWorkRequest request);
    void removeWorks(ExhibitionWorkRequest request);
    void sortWorks(ExhibitionWorkRequest request);
    List<ExhibitionWork> listWorks(Long exhibitionId);
}
