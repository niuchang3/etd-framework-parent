package org.etd.ai.knowledge;

import org.etd.ai.knowledge.biz.KnowledgeFolderBizService;
import org.etd.ai.knowledge.controller.KnowledgeFolderController;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证目录接口使用前端所选部门，而非当前用户主部门。 */
class KnowledgeFolderEndpointTest {
    private KnowledgeFolderService folders;
    private KnowledgeFolderBizService biz;
    private MockMvc mvc;

    /** 仅隔离业务服务，保留实际参数绑定和校验。 */
    @BeforeEach
    void createController() {
        folders = mock(KnowledgeFolderService.class);
        biz = mock(KnowledgeFolderBizService.class);
        var controller = new KnowledgeFolderController(folders, biz);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /** 未选择部门时委托权限范围查询，选择后参数原样传给查询能力。 */
    @Test
    void shouldAllowOptionalOrganizationOnList() throws Exception {
        var missing = get("/v1/knowledge-folders");
        mvc.perform(missing).andExpect(status().isBadRequest());
        verifyNoInteractions(folders);
        List<KnowledgeFolderEntity> empty = List.of();
        when(folders.fetchFolderList(777L, null)).thenReturn(empty);
        var selected = get("/v1/knowledge-folders").param("knowledgeBaseId", "777");
        mvc.perform(selected).andExpect(status().isOk());
        verify(folders).fetchFolderList(777L, null);
    }

    /** 不查询部门记录，所传部门ID由MapStruct转换后原样交给目录业务。 */
    @Test
    void shouldPassSelectedOrganizationOnCreation() throws Exception {
        String body = """
                {"knowledgeBaseId":"777","parentId":null,"folderName":"产品资料", "description":"说明","sortOrder":0}
                """;
        var request = post("/v1/knowledge-folders").contentType(MediaType.APPLICATION_JSON).content(body);
        mvc.perform(request).andExpect(status().isOk());
        var entity = ArgumentCaptor.forClass(KnowledgeFolderEntity.class);
        verify(biz).createFolder(entity.capture());
        assertEquals(777L, entity.getValue().getKnowledgeBaseId());
        assertEquals("说明", entity.getValue().getDescription());
        verifyNoInteractions(folders);
    }
}
