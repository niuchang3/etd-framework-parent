package org.etd.ai.agent;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import java.util.List;
import org.etd.ai.agent.biz.AgentBizService;
import org.etd.ai.agent.controller.AgentController;
import org.etd.ai.agent.controller.AgentOptionController;
import org.etd.ai.agent.entity.AgentEntity;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.ai.agent.service.AgentService;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.lang.reflect.Method;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证页面真实请求绑定、嵌套校验和输出隔离，业务持久化由独立PostgreSQL测试覆盖。 */
class AgentEndpointTest {
    /** 基础查询替身。 */
    private AgentService agents;
    /** 写入用例替身。 */
    private AgentBizService biz;
    /** 模型选项替身。 */
    private ModelConnectionService connections;
    /** 知识库选项替身。 */
    private KnowledgeBaseService knowledgeBases;
    /** 保留实际MVC绑定及校验的接口测试入口。 */
    private MockMvc mvc;

    /** 每例使用独立控制器，避免Mock调用记录互相影响。 */
    @BeforeEach
    void initialize() {
        agents = mock(AgentService.class);
        biz = mock(AgentBizService.class);
        connections = mock(ModelConnectionService.class);
        knowledgeBases = mock(KnowledgeBaseService.class);
        AgentController controller = new AgentController(agents, biz);
        AgentOptionController options = new AgentOptionController(connections, knowledgeBases);
        mvc = MockMvcBuilders.standaloneSetup(controller, options).build();
    }

    /** 页面可先创建空草稿；客户端提交的租户、状态、版本和发布配置不会进入待保存实体。 */
    @Test
    void shouldCreateIncompleteDraftWithoutAcceptingTrustedFields() throws Exception {
        AgentEntity saved = new AgentEntity();
        saved.setId(9L);
        saved.setVersion(0L);
        when(biz.createAgent(any())).thenReturn(saved);
        String body = """
                {"orgId":"10","name":"知识助手","isPublic":false,"draftConfig":{},
                 "tenantId":999,"dataStatus":0,"version":99,"publishedConfig":{"systemPrompt":"越权发布"}}
                """;
        MockHttpServletRequestBuilder request = post("/v1/agents").contentType(MediaType.APPLICATION_JSON).content(body);
        mvc.perform(request).andExpect(status().isOk());
        ArgumentCaptor<AgentEntity> captured = ArgumentCaptor.forClass(AgentEntity.class);
        verify(biz).createAgent(captured.capture());
        AgentEntity input = captured.getValue();
        assertEquals(10L, input.getOrgId());
        assertNull(input.getTenantId());
        assertNull(input.getDataStatus());
        assertNull(input.getPublishedConfig());
        assertNull(input.getVersion());
    }

    /** 更新及发布缺版本时返回400，嵌套温度和知识库ID校验不能被空草稿能力绕过。 */
    @Test
    void shouldRejectMissingVersionAndInvalidNestedConfiguration() throws Exception {
        String missing = """
                {"orgId":10,"name":"助手","isPublic":false,"draftConfig":{}}
                """;
        MockHttpServletRequestBuilder update = put("/v1/agents/1").contentType(MediaType.APPLICATION_JSON).content(missing);
        mvc.perform(update).andExpect(status().isBadRequest());
        MockHttpServletRequestBuilder publish = post("/v1/agents/1/publish").contentType(MediaType.APPLICATION_JSON).content("{}");
        mvc.perform(publish).andExpect(status().isBadRequest());
        String invalid = """
                {"orgId":10,"name":"助手","isPublic":false,
                 "draftConfig":{"modelOptions":{"temperature":3},"knowledgeBaseIds":[null]}}
                """;
        MockHttpServletRequestBuilder create = post("/v1/agents").contentType(MediaType.APPLICATION_JSON).content(invalid);
        mvc.perform(create).andExpect(status().isBadRequest());
        verifyNoInteractions(biz);
    }

    /** 公开基本详情即使底层记录有配置，也不能序列化到响应。 */
    @Test
    void shouldKeepInternalConfigurationOutOfBasicDetail() throws Exception {
        AgentEntity entity = new AgentEntity();
        entity.setId(1L);
        AgentConfiguration config = new AgentConfiguration();
        config.setSystemPrompt("内部指令");
        entity.setDraftConfig(config);
        entity.setPublishedConfig(config);
        when(agents.requireAgent(1L, false, false)).thenReturn(entity);
        MockHttpServletRequestBuilder request = get("/v1/agents/1");
        mvc.perform(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftConfig").doesNotExist())
                .andExpect(jsonPath("$.data.publishedConfig").doesNotExist());
    }

    /** 编辑配置必须显式要求菜单写权限；所有HTTP入口都声明中文操作日志。 */
    @Test
    void shouldDeclareWritePermissionForConfigurationAndLogsForEveryEndpoint() throws Exception {
        Method method = AgentController.class.getMethod("getConfiguration", Long.class);
        assertEquals(PermissionAction.WRITE, method.getAnnotation(Permission.class).action());
        for (Class<? extends Object> controller : List.of(AgentController.class, AgentOptionController.class)) {
            for (Method endpoint : controller.getDeclaredMethods()) {
                assertNotNull(endpoint.getAnnotation(AutoLog.class));
            }
        }
    }

    /** 选择接口只返回选择所需字段，不泄露模型连接地址、凭证或知识索引设置。 */
    @Test
    void shouldReturnMinimalOptions() throws Exception {
        ModelConnectionEntity connection = new ModelConnectionEntity();
        connection.setId(1L);
        connection.setModelName("聊天模型");
        connection.setBaseUrl("https://example.invalid");
        connection.setApiKeyCiphertext("not-a-real-secret");
        Page<ModelConnectionEntity> modelPage = new Page<ModelConnectionEntity>();
        List<ModelConnectionEntity> records = List.of(connection);
        modelPage.setRecords(records);
        when(connections.fetchChatConnectionPage(1, 20, null)).thenReturn(modelPage);
        MockHttpServletRequestBuilder models = get("/v1/agents/options/models");
        mvc.perform(models).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].modelName").value("聊天模型"))
                .andExpect(jsonPath("$.data.records[0].baseUrl").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].apiKeyCiphertext").doesNotExist());
        Page<KnowledgeBaseEntity> basePage = new Page<KnowledgeBaseEntity>();
        when(knowledgeBases.fetchAvailableBasePage(1, 20, null)).thenReturn(basePage);
        MockHttpServletRequestBuilder bases = get("/v1/agents/options/knowledge-bases");
        mvc.perform(bases).andExpect(status().isOk());
    }
}
