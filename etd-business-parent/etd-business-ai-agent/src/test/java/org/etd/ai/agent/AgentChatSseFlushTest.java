package org.etd.ai.agent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.etd.ai.agent.biz.AgentChatBizService;
import org.etd.ai.agent.biz.AgentRunBizService;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.controller.AgentChatController;
import org.etd.ai.agent.service.AgentConversationService;
import org.etd.ai.agent.service.AgentMessageService;
import org.etd.ai.agent.service.AgentService;
import org.etd.framework.starter.ai.runtime.AgentExecutionEngine;
import org.etd.framework.starter.ai.runtime.AgentRuntime;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.etd.framework.starter.ai.runtime.AgentChatService;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.runtime.spi.AgentChatPrepareService;
import org.etd.framework.starter.ai.runtime.spi.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.etd.framework.starter.ai.runtime.spi.AgentRunRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 真实Tomcat和HTTP客户端验证小于8KB的delta在模型完成前可读，不依赖MockMvc的内存响应。 */
class AgentChatSseFlushTest {
    /** 后一段模型输出由测试主动放行，避免用时间差猜测是否发生缓冲。 */
    @Test
    void flushesEachDeltaBeforeModelCompletes() {
        Sinks.Empty<Void> release = Sinks.empty();
        AgentRunRecordService records = mock(AgentRunRecordService.class);
        when(records.renewRunLease(anyString())).thenReturn(AgentRuntimeStatus.RUNNING);
        when(records.finishRun(any())).thenAnswer(call -> call.getArgument(0, AgentRuntimeResult.class));
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        AgentExecutionEngine engine = new AgentExecutionEngine();
        try (AgentRuntime runtime = new AgentRuntime(engine, properties, () -> List.of(records))) {
            AgentChatController controller = createController(runtime, properties, records, release);
            AutoConfigurations configurations = AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class,
                    DispatcherServletAutoConfiguration.class, JacksonAutoConfiguration.class,
                    HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class);
            WebApplicationContextRunner runner = new WebApplicationContextRunner(
                    AnnotationConfigServletWebServerApplicationContext::new);
            runner.withConfiguration(configurations).withPropertyValues("server.port=0")
                    .withBean(AgentChatController.class, () -> controller).run(context -> {
                        assertThat(context).hasNotFailed();
                        ServletWebServerApplicationContext application =
                                (ServletWebServerApplicationContext) context.getSourceApplicationContext();
                        try {
                            verifyHttpStream(application.getWebServer().getPort(), release, records);
                        } finally {
                            release.tryEmitEmpty();
                        }
                    });
        }
    }

    /** 仅替换业务数据准备及模型，运行调度、消息组装、Controller和SSE监听器均使用生产实现。 */
    private AgentChatController createController(AgentRuntime runtime, AgentRuntimeProperties properties,
            AgentRunRecordService records, Sinks.Empty<Void> release) {
        ChatModel model = mock(ChatModel.class);
        ChatResponse first = response("第一段");
        ChatResponse second = response("第二段");
        Flux<ChatResponse> firstPart = Flux.just(first);
        Flux<ChatResponse> lastPart = Flux.just(second);
        Flux<ChatResponse> remaining = release.asMono().thenMany(lastPart);
        Flux<ChatResponse> stream = Flux.concat(firstPart, remaining);
        when(model.stream(any(Prompt.class))).thenReturn(stream);
        AgentChatBizService biz = mock(AgentChatBizService.class);
        AgentChatPrepareService preparation = mock(AgentChatPrepareService.class);
        AgentChatPreparation data = new AgentChatPreparation();
        data.setExecutionId("3");
        data.setModelTenantId("1");
        data.setModelConnectionId("100");
        data.setSystemPrompt("测试指令");
        Map<String, Object> attributes = Map.of(AgentChatPolicy.SESSION_ID, 1L, AgentChatPolicy.MESSAGE_ID, 2L);
        data.setAttributes(attributes);
        when(preparation.prepareChat(any())).thenReturn(data);
        ModelClientRegistry models = mock(ModelClientRegistry.class);
        when(models.getChatModel("1", "100")).thenReturn(model);
        AgentKnowledgeQueryService knowledge = mock(AgentKnowledgeQueryService.class);
        AgentChatService chat = new AgentChatService(runtime, properties, models, preparation, knowledge, records);
        AgentRunBizService runBiz = mock(AgentRunBizService.class);
        AgentService agents = mock(AgentService.class);
        AgentConversationService conversations = mock(AgentConversationService.class);
        AgentMessageService messages = mock(AgentMessageService.class);
        return new AgentChatController(biz, runBiz, agents, conversations, messages, chat);
    }

    /** 客户端逐行读取而非聚合整个响应；读取超时可直接揭示被缓冲的事件。 */
    private void verifyHttpStream(int port, Sinks.Empty<Void> release, AgentRunRecordService records) throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + port + "/agent/chat/sessions/1/stream");
        String body = "{\"requestId\":\"flush-test\",\"content\":\"问题\"}";
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofString(body);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE).POST(publisher).build();
        HttpResponse.BodyHandler<InputStream> handler = HttpResponse.BodyHandlers.ofInputStream();
        try (HttpClient client = HttpClient.newHttpClient();
                ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor()) {
            HttpResponse<InputStream> response = client.sendAsync(request, handler).get(5, TimeUnit.SECONDS);
            InputStreamReader decoder = new InputStreamReader(response.body(), StandardCharsets.UTF_8);
            try (BufferedReader reader = new BufferedReader(decoder)) {
                verifyFirstDelta(response, reader, readers, records, release);
                Future<String> rest = readers.submit(() -> readUntil(reader, "event:done"));
                assertThat(rest.get(5, TimeUnit.SECONDS)).contains("第二段", "event:done");
            }
        }
    }

    /** 无论断言是否通过都先放行模型，保证失败时关闭读通道也不会等待未完成的生成。 */
    private void verifyFirstDelta(HttpResponse<InputStream> response, BufferedReader reader, ExecutorService readers,
            AgentRunRecordService records, Sinks.Empty<Void> release) throws Exception {
        try {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue(HttpHeaders.CACHE_CONTROL)).contains("no-cache, no-transform");
            assertThat(response.headers().firstValue("X-Accel-Buffering")).contains("no");
            assertThat(response.headers().firstValue(HttpHeaders.CONNECTION)).contains("keep-alive");
            Future<String> first = readers.submit(() -> readUntil(reader, "第一段"));
            assertThat(first.get(5, TimeUnit.SECONDS)).contains("event:delta", "第一段").doesNotContain("event:done");
            verify(records, never()).finishRun(any());
        } finally {
            release.tryEmitEmpty();
        }
    }

    /** 保留已读事件，达到目标即返回；连接未关闭时也必须能够读到第一段。 */
    private String readUntil(BufferedReader reader, String target) throws Exception {
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            text.append(line).append('\n');
            if (line.contains(target)) {
                break;
            }
        }
        return text.toString();
    }

    /** 构造小片段，确保测试不会依赖填满容器缓冲区才通过。 */
    private ChatResponse response(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        List<Generation> results = List.of(generation);
        return new ChatResponse(results);
    }
}
