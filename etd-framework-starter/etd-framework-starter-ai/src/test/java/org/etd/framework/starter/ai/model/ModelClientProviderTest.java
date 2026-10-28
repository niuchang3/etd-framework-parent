package org.etd.framework.starter.ai.model;

import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.ai.model.client.ModelRequestHeaders;
import java.util.concurrent.CopyOnWriteArrayList;
import org.etd.framework.starter.ai.model.client.ModelClientFactory;
import org.etd.framework.starter.ai.model.client.ModelClientTransport;
import org.etd.framework.starter.ai.model.config.ModelClientProperties;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.etd.framework.starter.ai.model.provider.DashScopeModelClientProvider;
import org.etd.framework.starter.ai.model.provider.OllamaModelClientProvider;
import org.etd.framework.starter.ai.model.provider.OpenAiModelClientProvider;
import org.etd.framework.starter.ai.model.spi.ModelClientProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import java.io.OutputStream;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.model.Model;

import static org.assertj.core.api.Assertions.*;

/** 真实SDK请求本地HTTP服务，验证协议路径、认证隔离和响应解析，不调用收费模型。 */
class ModelClientProviderTest {
    private final ObjectMapper json = new ObjectMapper();
    private final List<CapturedRequest> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private ModelClientTransport transport;
    private ModelClientFactory factory;
    /** 当前真实模型的两种序号字段；只控制本地服务的向量响应。 */
    private String embeddingIndexField = "text_index";

    /** 创建真实HTTP传输及生产适配器，初始化时不得发送任何请求。 */
    @BeforeEach
    void initialize() throws IOException {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
        server = HttpServer.create(address, 0);
        server.createContext("/", this::respond);
        server.start();
        ModelClientProperties properties = new ModelClientProperties();
        transport = new ModelClientTransport(properties);
        DashScopeModelClientProvider dashscope = new DashScopeModelClientProvider(transport, ObservationRegistry.NOOP);
        OpenAiModelClientProvider openai = new OpenAiModelClientProvider(transport, ObservationRegistry.NOOP);
        OllamaModelClientProvider ollama = new OllamaModelClientProvider(transport, ObservationRegistry.NOOP);
        List<ModelClientProvider> providers = List.of(dashscope, openai, ollama);
        factory = new ModelClientFactory(providers);
    }

    @AfterEach
    void cleanup() {
        server.stop(0);
        transport.close();
    }

    /** 三种协议均使用SDK完成聊天与向量请求，两个不同连接不串用凭证。 */
    @Test
    void shouldCallChatAndEmbeddingThroughAllBuiltInProtocols() {
        List<String> protocols = List.of(ModelClientProtocol.OPENAI_COMPATIBLE, ModelClientProtocol.DASHSCOPE, ModelClientProtocol.OLLAMA);
        for (String protocol : protocols) {
            ModelConnectionConfiguration chatConfig = configuration(protocol, ModelCapability.CHAT, "test-key-a");
            ModelConnectionConfiguration embeddingConfig = configuration(protocol, ModelCapability.EMBEDDING, "test-key-b");
            ChatModel model = createModel(chatConfig, ChatModel.class);
            EmbeddingModel vectors = createModel(embeddingConfig, EmbeddingModel.class);
            int previous = requests.size();
            assertThat(model.call("你好")).isEqualTo("本地响应");
            assertThat(vectors.embed("文本")).containsExactly(1.0f, 0.0f);
            assertThat(requests.get(previous).authorization()).isEqualTo("Bearer test-key-a");
            assertThat(requests.get(previous + 1).authorization()).isEqualTo("Bearer test-key-b");
        }
        assertThat(requests).hasSize(6);
        assertThat(requests).extracting(CapturedRequest::path).containsExactly(
                "/v1/chat/completions", "/v1/embeddings", "/api/v1/services/aigc/text-generation/generation",
                "/api/v1/services/embeddings/text-embedding/text-embedding", "/api/chat", "/api/embed");
    }

    /** 流式请求通过真实WebClient执行，独立于同步请求构建器，凭证仍属于当前连接。 */
    @Test
    void shouldStreamAllProtocolsAndSupportUnauthenticatedConnections() {
        List<String> protocols = List.of(ModelClientProtocol.OPENAI_COMPATIBLE, ModelClientProtocol.DASHSCOPE, ModelClientProtocol.OLLAMA);
        for (String protocol : protocols) {
            ModelConnectionConfiguration configuration = configuration(protocol, ModelCapability.CHAT, null);
            ChatModel model = createModel(configuration, ChatModel.class);
            Prompt prompt = new Prompt("问题");
            List<ChatResponse> result = model.stream(prompt).collectList().block(Duration.ofSeconds(5));
            assertThat(result).isNotEmpty();
            assertThat(result.getFirst().getResult().getOutput().getText()).isEqualTo("流式响应");
            assertThat(requests.getLast().authorization()).isNull();
        }
    }

    /** 请求级维度不污染共享默认选项，后续请求仍按自己的参数执行。 */
    @Test
    void shouldKeepEmbeddingDimensionsScopedToEachRequest() {
        ModelConnectionConfiguration configuration = configuration(ModelClientProtocol.OPENAI_COMPATIBLE, ModelCapability.EMBEDDING, null);
        EmbeddingModel model = createModel(configuration, EmbeddingModel.class);
        EmbeddingOptions options = EmbeddingOptions.builder().dimensions(2).build();
        List<String> texts = List.of("正文");
        EmbeddingRequest request = new EmbeddingRequest(texts, options);
        assertThat(model.call(request).getResults()).hasSize(1);
        assertThat(requests.getLast().body().path("dimensions").asInt()).isEqualTo(2);
        model.embed("另一份正文");
        assertThat(requests.getLast().body().path("dimensions").isMissingNode()
                || requests.getLast().body().path("dimensions").isNull()).isTrue();
    }

    /** 原生协议应保留服务端乱序下标，覆盖qwen3.7的index和text-embedding的text_index。 */
    @ParameterizedTest
    @ValueSource(strings = {"index", "text_index"})
    void shouldReadDashScopeEmbeddingIndices(String indexField) {
        embeddingIndexField = indexField;
        ModelConnectionConfiguration configuration = configuration(ModelClientProtocol.DASHSCOPE, ModelCapability.EMBEDDING, null);
        EmbeddingModel model = createModel(configuration, EmbeddingModel.class);
        EmbeddingOptions options = EmbeddingOptions.builder().dimensions(2).build();
        List<String> texts = List.of("正文甲", "正文乙");
        EmbeddingRequest request = new EmbeddingRequest(texts, options);
        List<Embedding> results = model.call(request).getResults();
        assertThat(results).hasSize(2);
        assertThat(results.getFirst().getIndex()).isEqualTo(1);
        assertThat(results.getFirst().getOutput()).containsExactly(1.0f, 0.0f);
        assertThat(results.getLast().getIndex()).isZero();
        assertThat(results.getLast().getOutput()).containsExactly(0.0f, 1.0f);
        assertThat(requests.getLast().body().path("parameters").path("dimension").asInt()).isEqualTo(2);
    }

    /** 原生重排仅匹配DashScope；OpenAI兼容接口不能被当作通用重排协议。 */
    @Test
    void shouldCreateDashScopeRerankAndRejectUnsupportedCombinations() {
        ModelConnectionConfiguration unsupported = configuration(ModelClientProtocol.OPENAI_COMPATIBLE, ModelCapability.RERANK, null);
        assertThatThrownBy(() -> factory.create(unsupported)).hasMessageContaining("没有支持");
        ModelConnectionConfiguration configuration = configuration(ModelClientProtocol.DASHSCOPE, ModelCapability.RERANK, "test-rerank-key");
        RerankModel model = createModel(configuration, RerankModel.class);
        assertThat(requests).isEmpty();
        Document document = new Document("文档正文");
        List<Document> documents = List.of(document);
        RerankRequest request = new RerankRequest("问题", documents);
        RerankResponse response = model.call(request);
        assertThat(response.getResults()).hasSize(1);
        assertThat(requests.getFirst().path()).isEqualTo("/api/v1/services/rerank/text-rerank/text-rerank");
        assertThat(requests.getFirst().authorization()).isEqualTo("Bearer test-rerank-key");
    }

    /** 同一缓存SDK按次透传请求头，前端Token不会覆盖供应商凭证，后续请求不残留前一租户。 */
    @Test
    void propagatesPerRequestHeadersWithoutChangingModelCredentials() {
        ModelConnectionConfiguration configuration = configuration(ModelClientProtocol.OPENAI_COMPATIBLE, ModelCapability.CHAT, "synthetic-model-key");
        ChatModel model = createModel(configuration, ChatModel.class);
        Prompt prompt = new Prompt("问题");
        try {
            RequestContext.setTenantCode(1L);
            RequestContext.setToken("synthetic-user-token");
            RequestContext.setTraceId("trace-one");
            RequestContext.setAttribute("x-business", "business-one");
            model.call(prompt);
            CapturedRequest first = requests.getLast();
            assertThat(first.headers()).containsEntry("tenant-code", "1").containsEntry("trace-id", "trace-one");
            assertThat(first.authorization()).isEqualTo("Bearer synthetic-model-key");
            RequestContext.setTenantCode(2L);
            RequestContext.setTraceId("trace-two");
            Map<String, String> headers = ModelRequestHeaders.capture();
            model.stream(prompt).contextWrite(context -> context.put(ModelRequestHeaders.REACTOR_KEY, headers))
                    .collectList().block(Duration.ofSeconds(5));
            assertThat(requests.getLast().headers()).containsEntry("tenant-code", "2").containsEntry("trace-id", "trace-two");
            assertThat(requests.getLast().authorization()).isEqualTo("Bearer synthetic-model-key");
            RequestContext.clean();
            model.call(prompt);
            assertThat(requests.getLast().headers()).doesNotContainKey("tenant-code");
        } finally {
            RequestContext.clean();
        }
    }

    /** 原生模型不需要租约或逐实例close，共享HTTP传输由测试上下文统一关闭。 */
    private <T> T createModel(ModelConnectionConfiguration configuration, Class<T> type) {
        Model<?, ?> model = factory.create(configuration);
        return type.cast(model);
    }

    private ModelConnectionConfiguration configuration(String protocol, ModelCapability capability, String key) {
        String path = switch (protocol) {
            case ModelClientProtocol.OPENAI_COMPATIBLE -> "/v1";
            case ModelClientProtocol.DASHSCOPE -> "/api/v1";
            default -> "";
        };
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
        return new ModelConnectionConfiguration("tenant", "connection", 1, true, protocol, capability,
                "test-model", uri, () -> key);
    }

    private void respond(HttpExchange exchange) throws IOException {
        JsonNode body = json.readTree(exchange.getRequestBody());
        String path = exchange.getRequestURI().getPath();
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        Map<String, String> headers = new HashMap<>();
        exchange.getRequestHeaders().forEach((key, values) -> headers.put(key.toLowerCase(Locale.ROOT), values.getFirst()));
        CapturedRequest captured = new CapturedRequest(path, authorization, body, headers);
        requests.add(captured);
        boolean stream = body.path("stream").asBoolean(false)
                || "enable".equals(exchange.getRequestHeaders().getFirst("X-DashScope-SSE"));
        String response = response(path, stream);
        if (path.endsWith("/text-embedding") && body.path("input").path("texts").size() == 2) {
            response = """
                    {"request_id":"1","output":{"embeddings":[
                    {"%s":1,"embedding":[1.0,0.0]}, {"%s":0,"embedding":[0.0,1.0]}]},
                    "usage":{"prompt_tokens":2,"total_tokens":2}}
                    """.formatted(embeddingIndexField, embeddingIndexField);
        }
        String contentType = stream ? "text/event-stream" : "application/json";
        if (stream && "/api/chat".equals(path)) { contentType = "application/x-ndjson"; }
        exchange.getResponseHeaders().set("Content-Type", contentType);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
    }

    private String response(String path, boolean stream) {
        if (stream && "/api/chat".equals(path)) {
            return "{\"model\":\"test-model\",\"created_at\":\"2026-10-25T00:00:00Z\",\"message\":{\"role\":\"assistant\",\"content\":\"流式响应\"},\"done\":true}\n";
        }
        if (stream && path.startsWith("/api/v1/")) {
            return "data: {\"request_id\":\"1\",\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"流式响应\"},\"finish_reason\":\"stop\"}]},\"usage\":{\"input_tokens\":1,\"output_tokens\":1}}\n\n";
        }
        if (stream) {
            return "data: {\"id\":\"1\",\"model\":\"test-model\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"流式响应\"},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n";
        }
        return switch (path) {
            case "/v1/chat/completions" -> """
                    {"id":"1","model":"test-model","choices":[{"index":0,"message":{"role":"assistant","content":"本地响应"},"finish_reason":"stop"}],"usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
                    """;
            case "/v1/embeddings" -> """
                    {"model":"test-model","data":[{"index":0,"embedding":[1.0,0.0]}],"usage":{"prompt_tokens":1,"total_tokens":1}}
                    """;
            case "/api/v1/services/aigc/text-generation/generation" -> """
                    {"request_id":"1","output":{"choices":[{"message":{"role":"assistant","content":"本地响应"},"finish_reason":"stop"}]},"usage":{"input_tokens":1,"output_tokens":1,"total_tokens":2}}
                    """;
            case "/api/v1/services/embeddings/text-embedding/text-embedding" -> """
                    {"request_id":"1","output":{"embeddings":[{"%s":0,"embedding":[1.0,0.0]}]},"usage":{"total_tokens":1}}
                    """.formatted(embeddingIndexField);
            case "/api/v1/services/rerank/text-rerank/text-rerank" -> """
                    {"request_id":"1","output":{"results":[{"index":0,"relevance_score":0.9}]},"usage":{"total_tokens":1}}
                    """;
            case "/api/chat" -> """
                    {"model":"test-model","created_at":"2026-10-25T00:00:00Z","message":{"role":"assistant","content":"本地响应"},"done":true,"done_reason":"stop","prompt_eval_count":1,"eval_count":1}
                    """;
            case "/api/embed" -> """
                    {"model":"test-model","embeddings":[[1.0,0.0]],"prompt_eval_count":1}
                    """;
            default -> throw new IllegalStateException("非预期SDK请求路径");
        };
    }

    /** 本地合成请求的断言快照，不包含真实密钥。 */
    private record CapturedRequest(String path, String authorization, JsonNode body, Map<String, String> headers) { }
}
