package org.etd.ai.knowledge;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.etd.ai.knowledge.biz.KnowledgeDocumentBatchDeleteBizService;
import org.etd.ai.knowledge.biz.KnowledgeDocumentBizService;
import org.etd.ai.knowledge.controller.KnowledgeDocumentController;
import org.etd.ai.knowledge.model.KnowledgeDocumentDeleteFailure;
import org.etd.ai.knowledge.model.KnowledgeDocumentDeleteResult;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.Serializable;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证批量删除的真实请求校验及MapStruct成功/失败明细转换。 */
class KnowledgeDocumentBatchDeleteEndpointTest {
    /** 不替代实际HTTP参数校验的批量业务替身。 */
    private KnowledgeDocumentBatchDeleteBizService batch;
    /** 实际MVC测试入口。 */
    private MockMvc mvc;

    /** 保留控制器和转换器，仅隔离数据库及对象存储。 */
    @BeforeEach
    void initialize() {
        KnowledgeDocumentService documents = mock(KnowledgeDocumentService.class);
        KnowledgeDocumentBizService single = mock(KnowledgeDocumentBizService.class);
        batch = mock(KnowledgeDocumentBatchDeleteBizService.class);
        KnowledgeDocumentController controller = new KnowledgeDocumentController(documents, single, batch);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /** 明确返回部分成功结果，前端不能只根据业务成功码判断全部删除成功。 */
    @Test
    void shouldReturnPartialFailureDetails() throws Exception {
        List<Long> ids = List.of(1L, 2L);
        KnowledgeDocumentDeleteFailure failure = new KnowledgeDocumentDeleteFailure(2L, "资料正在生成分片，请稍后操作");
        List<Long> successIds = List.of(1L);
        List<KnowledgeDocumentDeleteFailure> failures = List.of(failure);
        KnowledgeDocumentDeleteResult result = new KnowledgeDocumentDeleteResult(successIds, failures);
        when(batch.removeDocumentList(10L, ids)).thenReturn(result);
        String body = """
                {"knowledgeBaseId":"10","documentIds":["1","2"]}
                """;
        MockHttpServletRequestBuilder request = post("/v1/knowledge-documents/batch-delete").contentType(MediaType.APPLICATION_JSON)
                .content(body);
        mvc.perform(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successIds[0]").value(1))
                .andExpect(jsonPath("$.data.failures[0].id").value(2))
                .andExpect(jsonPath("$.data.failures[0].message").value("资料正在生成分片，请稍后操作"));
        verify(batch).removeDocumentList(10L, ids);
    }

    /** 空列表、非法元素、缺少知识库和超过100项都不能进入删除流程。 */
    @Test
    void shouldRejectInvalidPayloads() throws Exception {
        String empty = """
                {"knowledgeBaseId":10,"documentIds":[]}
                """;
        String nullElement = """
                {"knowledgeBaseId":10,"documentIds":[null]}
                """;
        String zero = """
                {"knowledgeBaseId":10,"documentIds":[0]}
                """;
        String missingBase = """
                {"documentIds":[1]}
                """;
        List<String> payloads = List.of(empty, nullElement, zero, missingBase);
        for (String body : payloads) {
            MockHttpServletRequestBuilder request = post("/v1/knowledge-documents/batch-delete").contentType(MediaType.APPLICATION_JSON).content(body);
            mvc.perform(request).andExpect(status().isBadRequest());
        }
        ArrayList<Long> ids = new ArrayList<Long>();
        for (long id = 1; id <= 101; id++) {
            ids.add(id);
        }
        Map<String, Serializable> input = Map.of("knowledgeBaseId", 10L, "documentIds", ids);
        JsonMapper json = JsonMapper.builder().build();
        String body = json.writeValueAsString(input);
        MockHttpServletRequestBuilder request = post("/v1/knowledge-documents/batch-delete").contentType(MediaType.APPLICATION_JSON).content(body);
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(batch);
    }
}
