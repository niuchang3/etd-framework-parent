package org.etd.ai.knowledge;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.ai.knowledge.biz.KnowledgeDocumentBizService;
import org.etd.ai.knowledge.controller.KnowledgeDocumentController;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.model.service.ModelConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证资料分页 Query DTO 的集合绑定、原参数保留及校验约束。 */
class KnowledgeDocumentPageEndpointTest {
    private KnowledgeDocumentService documents;
    private MockMvc mvc;

    /** 保留真实 MVC 参数转换和校验，查询服务不访问数据库。 */
    @BeforeEach
    void setUp() {
        documents = mock(KnowledgeDocumentService.class);
        var biz = mock(KnowledgeDocumentBizService.class);
        var connections = mock(ModelConnectionService.class);
        var controller = new KnowledgeDocumentController(documents, biz, connections);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var page = new Page<KnowledgeDocumentEntity>(1L, 10L);
        when(documents.fetchDocumentPage(anyLong(), anyLong(), any(), any(), any(), any(), any())).thenReturn(page);
    }

    /** 重复键和逗号分隔两种传参形式均绑定为目录列表，原单目录参数同时保留。 */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldBindFolderIds(boolean repeated) throws Exception {
        var request = get("/v1/knowledge-documents").param("folderId", "9")
                .param("keyword", "说明").param("uploadStatus", "completed").param("processingStatus", "ready");
        if (repeated) {
            request.param("folderIds", "1", "2");
        } else {
            request.param("folderIds", "1,2");
        }
        mvc.perform(request).andExpect(status().isOk());
        List<Long> folderIds = List.of(1L, 2L);
        verify(documents).fetchDocumentPage(1L, 10L, 9L, folderIds, "说明", "completed", "ready");
    }

    /** 原有单目录调用及分页参数继续按原字段名绑定。 */
    @Test
    void shouldKeepSingleFolderQuery() throws Exception {
        var request = get("/v1/knowledge-documents").param("folderId", "9")
                .param("current", "2").param("size", "20");
        mvc.perform(request).andExpect(status().isOk());
        verify(documents).fetchDocumentPage(2L, 20L, 9L, null, null, null, null);
    }

    /** 不传目录时维持原有不按目录筛选的入口行为。 */
    @Test
    void shouldKeepDefaultPage() throws Exception {
        var request = get("/v1/knowledge-documents");
        mvc.perform(request).andExpect(status().isOk());
        verify(documents).fetchDocumentPage(1L, 10L, null, null, null, null, null);
    }

    /** 非正整数或不能转换的集合元素不进入业务查询。 */
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1,0"})
    void shouldRejectInvalidFolderIds(String value) throws Exception {
        var request = get("/v1/knowledge-documents").param("folderIds", value);
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(documents);
    }

    /** DTO 替换分散参数后仍保留原分页和单目录校验。 */
    @ParameterizedTest
    @ValueSource(strings = {"current", "size", "folderId"})
    void shouldKeepExistingValidation(String parameter) throws Exception {
        var request = get("/v1/knowledge-documents").param(parameter, "0");
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(documents);
    }
}
