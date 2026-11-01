package com.picture_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.vo.PictureVO;
import com.picture_backend.service.PhotoWorkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CosmosControllerTest {
    @Mock private PhotoWorkService workService;

    @Test
    void oneWorkCanConnectToSeveralThemesWithoutDuplicatingTheWork() {
        CosmosController controller = new CosmosController();
        ReflectionTestUtils.setField(controller, "workService", workService);
        Page<PhotoWork> page = new Page<>(1, 300);
        page.setRecords(List.of(
                work(1L, "第一幅", List.of(picture("/one.jpg", "自然光"), picture("/two.jpg", "山与雾"))),
                work(2L, "第二幅", List.of(picture("/three.jpg", "自然光", "山与雾")))
        ));
        when(workService.page(1, 300, null, "hot")).thenReturn(page);

        Map<String, Object> data = controller.graph(300, 2, 8).getData();
        assertEquals(2, ((List<?>) data.get("hubs")).size());
        assertEquals(2, ((List<?>) data.get("leaves")).size());
        assertEquals(4, ((List<?>) data.get("edges")).size());
        assertTrue(((List<?>) data.get("edges")).stream().anyMatch(edge ->
                "山与雾".equals(((Map<?, ?>) edge).get("hub")) && "1".equals(((Map<?, ?>) edge).get("leaf"))));
    }

    private static PhotoWork work(Long id, String title, List<PictureVO> pictures) {
        PhotoWork work = new PhotoWork();
        work.setId(id);
        work.setTitle(title);
        work.setPictures(pictures);
        return work;
    }

    private static PictureVO picture(String url, String... tags) {
        PictureVO picture = new PictureVO();
        picture.setUrl(url);
        picture.setTags(List.of(tags));
        return picture;
    }
}
