package org.etd.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.io.IOException;
import jakarta.servlet.AsyncEvent;
import org.springframework.mock.web.MockAsyncContext;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.etd.ai.agent.biz.AgentChatBizService;
import org.etd.ai.agent.biz.AgentRunBizService;
import org.etd.framework.starter.ai.runtime.AgentRuntime;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.controller.AgentChatController;
import org.etd.ai.agent.service.AgentConversationService;
import org.etd.ai.agent.service.AgentMessageService;
import org.etd.ai.agent.service.AgentRunService;
import org.etd.ai.agent.service.AgentService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.PermissionAuthority;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.model.client.ModelRequestHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Flux;
import jakarta.servlet.AsyncListener;
import org.etd.ai.agent.entity.AgentConversationEntity;
import org.etd.ai.agent.entity.AgentMessageEntity;
import org.etd.ai.agent.entity.AgentRunEntity;
import org.etd.ai.agent.model.AgentChatExecution;
import org.etd.ai.agent.model.AgentCitation;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 专用空PostgreSQL库验证生产DDL、真实权限及事务、SSE增量和跨节点取消；模型使用确定性替身。 */
@EnabledIfEnvironmentVariable(named = "AI_CHAT_TEST_JDBC_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=chat-test",
        "spring.datasource.druid.url=${AI_CHAT_TEST_JDBC_URL}",
        "spring.datasource.druid.username=${AI_CHAT_TEST_DB_USER}",
        "spring.datasource.druid.password=${AI_CHAT_TEST_DB_PASSWORD}",
        "spring.datasource.druid.driver-class-name=org.postgresql.Driver",
        "etd.ai.knowledge.upload.cleanup-enabled=false",
        "etd.ai.document.docling.enabled=false",
        "management.health.redis.enabled=false",
        "logging.file=${java.io.tmpdir}/etd-chat-test"
})
class AgentChatIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AgentChatBizService biz;
    @Autowired private AgentRunBizService runBiz;
    @Autowired private AgentConversationService conversations;
    @Autowired private AgentMessageService messages;
    @Autowired private AgentRunService runs;
    @Autowired private AgentService agents;
    @Autowired private AgentRuntime runtime;
    @Autowired private AgentRuntimeProperties runtimeProperties;
    @MockitoBean private ModelClientRegistry clients;
    @LocalServerPort private int port;
    private ChatModel model;
    private MockMvc mvc;

    /** 只初始化明确传入且没有业务表的隔离数据库。 */
    @BeforeAll
    void initializeSchema() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setUrl(System.getenv("AI_CHAT_TEST_JDBC_URL"));
        source.setUsername(System.getenv("AI_CHAT_TEST_DB_USER"));
        source.setPassword(System.getenv("AI_CHAT_TEST_DB_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        assertThat(jdbc.queryForObject("select count(*) from pg_tables where schemaname = 'public'", Long.class)).isZero();
        ClassPathResource schema = new ClassPathResource("sql/ai.sql");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schema);
        populator.execute(jdbc.getDataSource());
    }

    /** 准备可公开使用的Agent及不同草稿、发布指令，不连接收费模型。 */
    @BeforeEach
    void prepare() {
        jdbc.execute("truncate ai_agent_message, ai_agent_run, ai_agent_conversation, ai_agent, ai_knowledge_chunk, ai_knowledge_document, ai_knowledge_base, ai_model_connection, ai_model");
        jdbc.execute("""
                insert into ai_model (id, provider_code, model_type, model_name, model_code)
                values (10, 'test', 'chat', '测试模型', 'chat-test');
                insert into ai_model_connection (id, tenant_id, model_id, protocol, base_url, auth_type)
                values (100, 1, 10, 'openai_compatible', 'https://example.invalid', 'none');
                insert into ai_agent (id, tenant_id, org_id, name, is_public, create_by, draft_config, published_config)
                values (1, 1, 10, '测试助手', true, 7,
                '{"modelConnectionId":100,"systemPrompt":"草稿指令","knowledgeBaseIds":[]}',
                '{"modelConnectionId":100,"systemPrompt":"发布指令","knowledgeBaseIds":[]}');
                """);
        identity(7L, 1L);
        model = mock(ChatModel.class);
        when(clients.getChatModel("1", "100")).thenReturn(model);
        when(model.stream(any(Prompt.class))).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(RequestContext.getTenantCode()).isEqualTo(1L);
            return Flux.deferContextual(context -> {
                Map<String, String> headers = context.<Map<String, String>>get(ModelRequestHeaders.REACTOR_KEY);
                assertThat(headers).containsEntry("trace-id", "chat-test-trace");
                assertThat(headers).doesNotContainKey("Authorization");
                ChatResponse first = response("你好");
                ChatResponse second = response("，知识助手");
                return Flux.just(first, second).delayElements(Duration.ofMillis(20));
            });
        });
        AgentChatController controller = new AgentChatController(biz, runBiz, agents, conversations, messages, runtime, runtimeProperties);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /** 清理测试线程身份；运行线程由流的终态自行释放。 */
    @AfterEach
    void clear() {
        RequestContext.clean();
        SecurityContextHolder.clearContext();
    }

    /** 实际SSE包含增量，完整正文和幂等状态在流结束前持久化，正式聊天使用发布指令。 */
    @Test
    void streamsAndPersistsPublishedAnswerWithoutReexecutingDuplicate() throws Exception {
        AgentConversationEntity session = biz.createSession(1L, AgentChatPolicy.CHAT, "测试会话");
        MvcResult result = start(session.getId(), "request-one");
        String events = complete(result);
        assertThat(events).contains("event:start", "event:references", "event:delta", "event:done", "你好", "知识助手");
        AgentRunEntity run = runBiz.requireOwnedRunByRequest(session.getId(), "request-one");
        assertThat(run.getRunStatus()).isEqualTo(AgentChatPolicy.SUCCEEDED);
        List<AgentMessageEntity> history = messages.fetchMessageList(session.getId(), null, 10);
        assertThat(history).hasSize(2);
        assertThat(history.getLast().getContent()).isEqualTo("你好，知识助手");
        assertThat(history.getLast().getMessageStatus()).isEqualTo(AgentChatPolicy.COMPLETED);
        assertThatThrownBy(() -> biz.beginRun(session.getId(), "request-one", "重复"))
                .isInstanceOf(ApiRuntimeException.class).hasMessageContaining("请求已提交");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(1)).stream(prompt.capture());
        assertThat(prompt.getValue().getContents()).contains("发布指令").doesNotContain("草稿指令");
    }

    /** 真实知识SQL、上下文注入、SSE引用及JSONB持久化贯通，展示接口不泄漏内部指令。 */
    @Test
    void retrievesKnowledgeAndPersistsCitationSources() throws Exception {
        jdbc.execute("""
                insert into ai_knowledge_base (id, tenant_id, org_id, create_by, name, retrieval_mode)
                values (200, 1, 10, 7, '设备库', 'keyword');
                insert into ai_knowledge_document
                    (id, knowledge_base_id, tenant_id, org_id, create_by, folder_id, document_name, original_filename,
                     file_type, storage_bucket, storage_object_key, content_type, file_size, file_sha256,
                     part_size_bytes, part_count, upload_expires_at, upload_status, uploaded_time,
                     chunk_status, chunk_count, chunk_confirmed_time, data_status)
                values (300, 200, 1, 10, 7, 1, '安装指南', 'guide.md', 'md', 'test', 'test', 'text/markdown',
                        100, repeat('0',64), 10485760, 1, current_timestamp + interval '1 day',
                        'completed', current_timestamp, 'confirmed', 1, current_timestamp, 1);
                insert into ai_knowledge_chunk
                    (id, knowledge_base_id, tenant_id, org_id, create_by, document_id, chunk_index, content, token_count, source)
                values (400, 200, 1, 10, 7, 300, 0, '问题解决方法：先安装驱动再连接设备', 20,
                        '{"headings":["安装"],"pageNumbers":[2],"documentItemRefs":[]}');
                update ai_agent set published_config = published_config || '{"knowledgeBaseIds":[200],"openingMessage":"欢迎使用"}'::jsonb
                where id=1;
                """);
        AgentConversationEntity session = biz.createSession(1L, AgentChatPolicy.CHAT, "知识会话");
        MvcResult result = start(session.getId(), "rag-request");
        assertThat(complete(result)).contains("event:references", "安装指南", "pageNumbers");
        AgentRunEntity run = runBiz.requireOwnedRunByRequest(session.getId(), "rag-request");
        List<AgentCitation> citations = messages.fetchAssistant(run.getId()).getCitations();
        assertThat(citations).hasSize(1);
        assertThat(citations.getFirst().getNumber()).isEqualTo(1);
        assertThat(citations.getFirst().getSource().pageNumbers()).containsExactly(2);
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).stream(prompt.capture());
        assertThat(prompt.getValue().getContents()).contains("先安装驱动再连接设备");
        MockHttpServletRequestBuilder profile = get("/agent/chat/agents/1");
        mvc.perform(profile).andExpect(status().isOk()).andExpect(jsonPath("$.data.openingMessage").value("欢迎使用"))
                .andExpect(jsonPath("$.data.systemPrompt").doesNotExist());
    }

    /** 直接修改数据库模拟其他实例请求取消，当前节点停止上游订阅并保留已生成文本。 */
    @Test
    void cancelsAcrossNodesAndDisposesModelSubscription() throws Exception {
        AtomicBoolean cancelled = new AtomicBoolean();
        ChatResponse first = response("已生成部分");
        when(model.stream(any(Prompt.class))).thenReturn(Flux.concat(Flux.just(first), Flux.never())
                .doOnCancel(() -> cancelled.set(true)));
        AgentConversationEntity session = biz.createSession(1L, AgentChatPolicy.CHAT, "取消会话");
        MvcResult result = start(session.getId(), "request-cancel");
        awaitContent(result, "已生成部分");
        AgentRunEntity run = runBiz.requireOwnedRunByRequest(session.getId(), "request-cancel");
        jdbc.update("update ai_agent_run set run_status='cancel_requested', cancel_requested_time=current_timestamp where id=?", run.getId());
        String events = complete(result);
        assertThat(events).contains("event:done", "cancelled");
        assertThat(cancelled).isTrue();
        assertThat(runs.requireRun(run.getId()).getRunStatus()).isEqualTo(AgentChatPolicy.CANCELLED);
        assertThat(messages.fetchAssistant(run.getId()).getMessageStatus()).isEqualTo(AgentChatPolicy.INTERRUPTED);
        assertThat(messages.fetchAssistant(run.getId()).getContent()).isEqualTo("已生成部分");
    }

    /** SSE连接发生网络错误后取消订阅，保留部分正文并释放活跃执行。 */
    @Test
    void disconnectCancelsGenerationAndKeepsPartialMessage() throws Exception {
        AtomicBoolean cancelled = new AtomicBoolean();
        ChatResponse first = response("断线前内容");
        when(model.stream(any(Prompt.class))).thenReturn(Flux.concat(Flux.just(first), Flux.never())
                .doOnCancel(() -> cancelled.set(true)));
        AgentConversationEntity session = biz.createSession(1L, AgentChatPolicy.CHAT, "断线会话");
        MvcResult result = start(session.getId(), "disconnect-request");
        awaitContent(result, "断线前内容");
        MockAsyncContext context = (MockAsyncContext) result.getRequest().getAsyncContext();
        IOException failure = new IOException("synthetic-disconnect");
        AsyncEvent event = new AsyncEvent(context, failure);
        for (AsyncListener listener : context.getListeners()) {
            listener.onError(event);
        }
        AgentRunEntity run = runBiz.requireOwnedRunByRequest(session.getId(), "disconnect-request");
        awaitTerminal(run.getId());
        assertThat(cancelled).isTrue();
        assertThat(runs.requireRun(run.getId()).getRunStatus()).isEqualTo(AgentChatPolicy.CANCELLED);
        assertThat(messages.fetchAssistant(run.getId()).getContent()).isEqualTo("断线前内容");
    }

    /** 两个租户同时使用共享执行器，各自模型调用、Reactor请求头及最终正文互不串用。 */
    @Test
    void concurrentStreamsKeepTenantContextIsolated() throws Exception {
        jdbc.execute("""
                insert into ai_model_connection (id, tenant_id, model_id, protocol, base_url, auth_type)
                values (101, 2, 10, 'openai_compatible', 'https://example.invalid', 'none');
                insert into ai_agent (id, tenant_id, org_id, name, is_public, create_by, draft_config, published_config)
                values (2, 2, 10, '第二租户助手', true, 8, '{}',
                '{"modelConnectionId":101,"systemPrompt":"第二租户指令","knowledgeBaseIds":[]}');
                """);
        when(clients.getChatModel(any(), any())).thenReturn(model);
        when(model.stream(any(Prompt.class))).thenAnswer(call -> {
            String tenant = RequestContext.getTenantCode().toString();
            return Flux.deferContextual(context -> {
                Map<String, String> headers = context.<Map<String, String>>get(ModelRequestHeaders.REACTOR_KEY);
                assertThat(headers).containsEntry("tenant-code", tenant);
                ChatResponse value = response("租户" + tenant + "的答案");
                return Flux.just(value).delayElements(Duration.ofMillis(150));
            });
        });
        AgentConversationEntity first = biz.createSession(1L, AgentChatPolicy.CHAT, "第一租户");
        MvcResult firstResult = start(first.getId(), "parallel-one");
        identity(8L, 2L);
        AgentConversationEntity second = biz.createSession(2L, AgentChatPolicy.CHAT, "第二租户");
        MvcResult secondResult = start(second.getId(), "parallel-two");
        assertThat(complete(firstResult)).contains("租户1的答案").doesNotContain("租户2的答案");
        assertThat(complete(secondResult)).contains("租户2的答案").doesNotContain("租户1的答案");
    }

    /** 模型在部分输出后失败，正文保留，错误事件不带原始SDK异常。 */
    @Test
    void keepsPartialAnswerAndSanitizesProviderFailure() throws Exception {
        ChatResponse first = response("部分答案");
        IllegalStateException failure = new IllegalStateException("secret-from-provider");
        when(model.stream(any(Prompt.class))).thenReturn(Flux.concat(Flux.just(first), Flux.error(failure)));
        AgentConversationEntity session = biz.createSession(1L, AgentChatPolicy.CHAT, "失败会话");
        MvcResult result = start(session.getId(), "request-failed");
        String events = complete(result);
        assertThat(events).contains("event:error").doesNotContain("secret-from-provider");
        AgentRunEntity run = runBiz.requireOwnedRunByRequest(session.getId(), "request-failed");
        assertThat(run.getRunStatus()).isEqualTo(AgentChatPolicy.FAILED);
        assertThat(messages.fetchAssistant(run.getId()).getContent()).isEqualTo("部分答案");
    }

    /** 个人会话不随Agent公开，普通用户不能跨用户或跨租户读取、停止。 */
    @Test
    void isolatesOwnedSessionsAndAllowsPlatformAdministration() {
        AgentConversationEntity session = biz.createSession(1L, AgentChatPolicy.CHAT, "个人会话");
        AgentChatExecution execution = biz.beginRun(session.getId(), "owned-request", "问题");
        identity(8L, 1L);
        assertThatThrownBy(() -> conversations.requireConversation(session.getId(), false)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> runBiz.cancelRun(execution.run().getId())).isInstanceOf(ApiRuntimeException.class);
        identity(7L, 2L);
        assertThatThrownBy(() -> conversations.requireConversation(session.getId(), false)).isInstanceOf(ApiRuntimeException.class);
        RequestContext.getUser().setPlatformAdmin(true);
        assertThat(conversations.requireConversation(session.getId(), false).getId()).isEqualTo(session.getId());
    }

    /** 草稿调试同时要求写权限与数据维护资格。 */
    @Test
    void requiresWritePermissionForDraftDebug() {
        assertThatThrownBy(() -> biz.createSession(1L, AgentChatPolicy.DEBUG, "调试"))
                .hasMessageContaining("菜单写权限");
        PermissionAuthority write = new PermissionAuthority("ai:agent:write");
        RequestContext.getUser().setAuthorities(List.of(write));
        AgentConversationEntity session = biz.createSession(1L, AgentChatPolicy.DEBUG, "调试");
        AgentChatExecution execution = biz.beginRun(session.getId(), "debug-request", "问题");
        assertThat(execution.configuration().getSystemPrompt()).isEqualTo("草稿指令");
        RequestContext.getUser().setAuthorities(List.of());
        assertThatThrownBy(() -> biz.beginRun(session.getId(), "next-request", "问题"))
                .isInstanceOf(ApiRuntimeException.class);
    }

    /** 完成与取消竞争由数据库决定；终态重复提交不会覆盖最终正文。 */
    @Test
    void cancellationWinsAndTerminalUpdatesAreIdempotent() {
        AgentConversationEntity session = biz.createSession(1L, AgentChatPolicy.CHAT, "竞争会话");
        AgentChatExecution execution = biz.beginRun(session.getId(), "race", "问题");
        runBiz.cancelRun(execution.run().getId());
        execution.run().setRunStatus(AgentChatPolicy.SUCCEEDED);
        assertThat(runBiz.finishRun(execution.run(), "部分", List.of())).isEqualTo(AgentChatPolicy.CANCELLED);
        execution.run().setRunStatus(AgentChatPolicy.FAILED);
        runBiz.finishRun(execution.run(), "错误覆盖", List.of());
        assertThat(messages.fetchAssistant(execution.run().getId()).getContent()).isEqualTo("部分");
    }

    /** 过期租约清理消息状态并释放活跃位置，不自动调用模型。 */
    @Test
    void expiresLostRunsWithoutReplayingModel() {
        AgentConversationEntity session = biz.createSession(1L, AgentChatPolicy.CHAT, "失联会话");
        AgentChatExecution execution = biz.beginRun(session.getId(), "lost", "问题");
        jdbc.update("update ai_agent_run set lease_until=current_timestamp - interval '1 minute' where id=?", execution.run().getId());
        runBiz.expireRunList();
        assertThat(runs.requireRun(execution.run().getId()).getErrorCode()).isEqualTo(AgentChatPolicy.LEASE_EXPIRED);
        assertThat(messages.fetchAssistant(execution.run().getId()).getMessageStatus()).isEqualTo(AgentChatPolicy.FAILED);
        assertThat(biz.beginRun(session.getId(), "new", "新问题").run().getRunStatus()).isEqualTo(AgentChatPolicy.RUNNING);
        verifyNoInteractions(model);
    }

    /** 实际服务器继续保护聊天接口，公开健康检查仍然可读。 */
    @Test
    void protectsChatWhileKeepingHealthPublic() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            URI chatUri = URI.create("http://127.0.0.1:" + port + "/agent/chat/sessions");
            HttpRequest chat = HttpRequest.newBuilder(chatUri).GET().build();
            assertThat(client.send(chat, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
            URI healthUri = URI.create("http://127.0.0.1:" + port + "/actuator/health");
            HttpRequest health = HttpRequest.newBuilder(healthUri).GET().build();
            assertThat(client.send(health, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        }
    }

    private void identity(Long userId, Long tenant) {
        RequestContext.clean();
        UserDetails user = new UserDetails();
        user.setId(userId);
        user.setOrgId(10L);
        user.setScopeOrgIds(Set.of(10L));
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode()));
        RequestContext.setUser(user);
        RequestContext.setTenantCode(tenant);
        RequestContext.setTraceId("chat-test-trace");
        RequestContext.setToken("synthetic-user-token");
    }

    private ChatResponse response(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        List<Generation> results = List.of(generation);
        return new ChatResponse(results);
    }

    private MvcResult start(Long sessionId, String requestId) throws Exception {
        String body = new ObjectMapper().createObjectNode().put("requestId", requestId).put("content", "问题").toString();
        MockHttpServletRequestBuilder request = post("/agent/chat/sessions/" + sessionId + "/stream")
                .contentType("application/json").content(body);
        return mvc.perform(request).andExpect(request().asyncStarted()).andReturn();
    }

    private String complete(MvcResult result) throws Exception {
        result.getAsyncResult(10000);
        RequestBuilder request = asyncDispatch(result);
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private void awaitTerminal(Long id) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (AgentChatPolicy.RUNNING.equals(runs.requireRun(id).getRunStatus()) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(runs.requireRun(id).getRunStatus()).isNotEqualTo(AgentChatPolicy.RUNNING);
    }

    private void awaitContent(MvcResult result, String text) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!result.getResponse().getContentAsString(StandardCharsets.UTF_8).contains(text) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(text);
    }
}
