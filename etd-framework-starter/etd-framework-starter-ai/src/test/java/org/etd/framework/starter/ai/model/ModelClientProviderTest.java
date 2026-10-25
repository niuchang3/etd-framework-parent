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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;

import static org.assertj.core.api.Assertions.*;

/** 真实SDK请求本地HTTP服务，验证协议路径、认证隔离和响应解析，不调用收费模型。 */
class ModelClientProviderTest {
    private final ObjectMapper json = new ObjectMapper();
    private final List<CapturedRequest> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private ModelClientTransport transport;
    private ModelClientFactory factory;

    /** 创建真实HTTP传输及生产适配器，初始化时不得发送任何请求。 */
    @BeforeEach
    void initialize() throws IOException {
        var address = new InetSocketAddress("127.0.0.1", 0);
        server = HttpServer.create(address, 0);
        server.createContext("/", this::respond);
        server.start();
        var properties = new ModelClientProperties();
        transport = new ModelClientTransport(properties);
        var dashscope = new DashScopeModelClientProvider(transport, ObservationRegistry.NOOP);
        var openai = new OpenAiModelClientProvider(transport, ObservationRegistry.NOOP);
        var ollama = new OllamaModelClientProvider(transport, ObservationRegistry.NOOP);
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
        var protocols = List.of(ModelClientProtocol.OPENAI_COMPATIBLE, ModelClientProtocol.DASHSCOPE, ModelClientProtocol.OLLAMA);
        for (var protocol : protocols) {
            var chatConfig = configuration(protocol, ModelCapability.CHAT, "test-key-a");
            var embeddingConfig = configuration(protocol, ModelCapability.EMBEDDING, "test-key-b");
            var model = createModel(chatConfig, ChatModel.class);
            var vectors = createModel(embeddingConfig, EmbeddingModel.class);
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
        var protocols = List.of(ModelClientProtocol.OPENAI_COMPATIBLE, ModelClientProtocol.DASHSCOPE, ModelClientProtocol.OLLAMA);
        for (var protocol : protocols) {
            var configuration = configuration(protocol, ModelCapability.CHAT, null);
            var model = createModel(configuration, ChatModel.class);
            var prompt = new Prompt("问题");
            var result = model.stream(prompt).collectList().block(Duration.ofSeconds(5));
            assertThat(result).isNotEmpty();
            assertThat(result.getFirst().getResult().getOutput().getText()).isEqualTo("流式响应");
            assertThat(requests.getLast().authorization()).isNull();
        }
    }

    /** 请求级维度不污染共享默认选项，后续请求仍按自己的参数执行。 */
    @Test
    void shouldKeepEmbeddingDimensionsScopedToEachRequest() {
        var configuration = configuration(ModelClientProtocol.OPENAI_COMPATIBLE, ModelCapability.EMBEDDING, null);
        var model = createModel(configuration, EmbeddingModel.class);
        var options = EmbeddingOptions.builder().dimensions(2).build();
        var texts = List.of("正文");
        var request = new EmbeddingRequest(texts, options);
        assertThat(model.call(request).getResults()).hasSize(1);
        assertThat(requests.getLast().body().path("dimensions").asInt()).isEqualTo(2);
        model.embed("另一份正文");
        assertThat(requests.getLast().body().path("dimensions").isMissingNode()
                || requests.getLast().body().path("dimensions").isNull()).isTrue();
    }

    /** 原生重排仅匹配DashScope；OpenAI兼容接口不能被当作通用重排协议。 */
    @Test
    void shouldCreateDashScopeRerankAndRejectUnsupportedCombinations() {
        var unsupported = configuration(ModelClientProtocol.OPENAI_COMPATIBLE, ModelCapability.RERANK, null);
        assertThatThrownBy(() -> factory.create(unsupported)).hasMessageContaining("没有支持");
        var configuration = configuration(ModelClientProtocol.DASHSCOPE, ModelCapability.RERANK, "test-rerank-key");
        var model = createModel(configuration, RerankModel.class);
        assertThat(requests).isEmpty();
        var document = new Document("文档正文");
        var documents = List.of(document);
        var request = new RerankRequest("问题", documents);
        var response = model.call(request);
        assertThat(response.getResults()).hasSize(1);
        assertThat(requests.getFirst().path()).isEqualTo("/api/v1/services/rerank/text-rerank/text-rerank");
        assertThat(requests.getFirst().authorization()).isEqualTo("Bearer test-rerank-key");
    }

    /** 原生模型不需要租约或逐实例close，共享HTTP传输由测试上下文统一关闭。 */
    private <T> T createModel(ModelConnectionConfiguration configuration, Class<T> type) {
        var model = factory.create(configuration);
        return type.cast(model);
    }

    private ModelConnectionConfiguration configuration(String protocol, ModelCapability capability, String key) {
        String path = switch (protocol) {
            case ModelClientProtocol.OPENAI_COMPATIBLE -> "/v1";
            case ModelClientProtocol.DASHSCOPE -> "/api/v1";
            default -> "";
        };
        var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
        return new ModelConnectionConfiguration("tenant", "connection", 1, true, protocol, capability,
                "test-model", uri, () -> key);
    }

    private void respond(HttpExchange exchange) throws IOException {
        var body = json.readTree(exchange.getRequestBody());
        String path = exchange.getRequestURI().getPath();
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        var captured = new CapturedRequest(path, authorization, body);
        requests.add(captured);
        boolean stream = body.path("stream").asBoolean(false)
                || "enable".equals(exchange.getRequestHeaders().getFirst("X-DashScope-SSE"));
        String response = response(path, stream);
        String contentType = stream ? "text/event-stream" : "application/json";
        if (stream && "/api/chat".equals(path)) { contentType = "application/x-ndjson"; }
        exchange.getResponseHeaders().set("Content-Type", contentType);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
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
                    {"request_id":"1","output":{"embeddings":[{"text_index":0,"embedding":[1.0,0.0]}]},"usage":{"total_tokens":1}}
                    """;
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
    private record CapturedRequest(String path, String authorization, JsonNode body) { }
}
