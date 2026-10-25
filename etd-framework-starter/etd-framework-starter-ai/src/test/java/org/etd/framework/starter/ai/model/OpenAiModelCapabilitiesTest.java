package org.etd.framework.starter.ai.model;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.etd.framework.starter.ai.model.client.ModelClientFactory;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.model.client.ModelClientTransport;
import org.etd.framework.starter.ai.model.config.ModelClientProperties;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.etd.framework.starter.ai.model.provider.DashScopeModelClientProvider;
import org.etd.framework.starter.ai.model.provider.OllamaModelClientProvider;
import org.etd.framework.starter.ai.model.provider.OpenAiModelClientProvider;
import org.etd.framework.starter.ai.model.spi.ModelConnectionResolver;
import org.etd.framework.starter.ai.model.spi.ModelClientProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.OpenAiModerationOptions;
import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.*;

/** 使用真实SDK和本地HTTP服务验证新增能力、根路径、认证、请求选项及缓存，不调用收费服务。 */
class OpenAiModelCapabilitiesTest {
    private final ObjectMapper json = new ObjectMapper();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private ModelClientTransport transport;
    private ModelClientFactory factory;
    private final ModelClientProperties properties = new ModelClientProperties();
    private int responseStatus;

    /** 模拟JSON、multipart上传和二进制音频端点；创建客户端本身不得触发请求。 */
    @BeforeEach
    void initialize() throws IOException {
        var address = new InetSocketAddress("127.0.0.1", 0);
        server = HttpServer.create(address, 0);
        server.createContext("/", this::respond);
        server.start();
        transport = new ModelClientTransport(properties);
        var provider = new OpenAiModelClientProvider(transport, ObservationRegistry.NOOP);
        List<ModelClientProvider> providers = List.of(provider);
        factory = new ModelClientFactory(providers);
        responseStatus = 200;
    }

    @AfterEach
    void cleanup() {
        server.stop(0);
        transport.close();
    }

    /** 所有新增能力统一授权、按需创建并缓存；保留标准、代理以及非v1根路径。 */
    @ParameterizedTest
    @ValueSource(strings = {"/v1", "/proxy/openai/v1/", "/gateway/api"})
    void shouldCallNewCapabilitiesWithConfiguredApiRoot(String apiRoot) throws Exception {
        var authorized = new AtomicInteger();
        ModelConnectionResolver resolver = (tenant, id) -> {
            authorized.incrementAndGet();
            var capability = ModelCapability.valueOf(id);
            return configuration(tenant, capability, apiRoot, "test-" + id);
        };
        try (var registry = new ModelClientRegistry(factory, resolver, properties)) {
            callImage(registry);
            callTranscription(registry);
            callSpeech(registry);
            callModeration(registry);
            assertThat(authorized).hasValue(8);
            assertThat(requests).hasSize(5);
            String root = apiRoot.endsWith("/") ? apiRoot.substring(0, apiRoot.length() - 1) : apiRoot;
            assertThat(requests).extracting(Request::path).containsExactly(root + "/images/generations",
                    root + "/audio/transcriptions", root + "/audio/speech", root + "/audio/speech", root + "/moderations");
            assertRequestModelsAndAuth();
        }
    }

    private void callImage(ModelClientRegistry registry) {
        var model = registry.getImageModel("tenant", ModelCapability.IMAGE.name());
        assertThat(requests).isEmpty();
        assertThat(registry.getImageModel("tenant", ModelCapability.IMAGE.name())).isSameAs(model);
        var prompt = new ImagePrompt("测试图片");
        assertThat(model.call(prompt).getResult().getOutput().getUrl()).isEqualTo("https://example.invalid/image.png");
    }

    private void callTranscription(ModelClientRegistry registry) {
        var model = registry.getTranscriptionModel("tenant", ModelCapability.SPEECH_TO_TEXT.name());
        assertThat(registry.getTranscriptionModel("tenant", ModelCapability.SPEECH_TO_TEXT.name())).isSameAs(model);
        var audio = audioResource();
        var prompt = new AudioTranscriptionPrompt(audio);
        assertThat(model.call(prompt).getResult().getOutput()).isEqualTo("测试转录");
    }

    private void callSpeech(ModelClientRegistry registry) {
        var model = registry.getTextToSpeechModel("tenant", ModelCapability.TEXT_TO_SPEECH.name());
        assertThat(registry.getTextToSpeechModel("tenant", ModelCapability.TEXT_TO_SPEECH.name())).isSameAs(model);
        var prompt = new TextToSpeechPrompt("测试合成");
        assertThat(model.call(prompt).getResult().getOutput()).isEqualTo(audioBytes());
        var streamed = model.stream(prompt).collectList().block(Duration.ofSeconds(5));
        assertThat(streamed).isNotEmpty();
        int byteCount = 0;
        for (var part : streamed) {
            byteCount += part.getResult().getOutput().length;
        }
        assertThat(byteCount).isEqualTo(audioBytes().length);
    }

    private void callModeration(ModelClientRegistry registry) {
        var model = registry.getModerationModel("tenant", ModelCapability.MODERATION.name());
        assertThat(registry.getModerationModel("tenant", ModelCapability.MODERATION.name())).isSameAs(model);
        var prompt = new ModerationPrompt("测试审核");
        var output = model.call(prompt).getResult().getOutput();
        assertThat(output.getResults()).hasSize(1);
        assertThat(output.getResults().getFirst().isFlagged()).isFalse();
    }

    private void assertRequestModelsAndAuth() throws Exception {
        for (var request : requests) {
            assertThat(request.method()).isEqualTo("POST");
            if (request.path().endsWith("/audio/transcriptions")) {
                assertThat(request.authorization()).isEqualTo("Bearer test-SPEECH_TO_TEXT");
                assertThat(request.contentType()).startsWith("multipart/form-data;");
                assertThat(request.body()).contains("filename=\"sample.wav\"", "test-SPEECH_TO_TEXT", "json");
            } else {
                var body = json.readTree(request.body());
                assertThat(request.authorization()).isEqualTo("Bearer " + body.get("model").asText());
            }
        }
    }

    /** 每次请求覆盖模型与音色等参数后，后续请求仍使用连接默认项；无密钥时不发送认证头。 */
    @Test
    void shouldKeepRequestOptionsIsolatedWithoutAuthentication() throws Exception {
        ModelConnectionResolver resolver = (tenant, id) -> configuration(tenant, ModelCapability.valueOf(id), "/v1", null);
        try (var registry = new ModelClientRegistry(factory, resolver, properties)) {
            callImageWithOptions(registry);
            callSpeechWithOptions(registry);
            callTranscriptionWithOptions(registry);
            var moderation = registry.getModerationModel("tenant", "MODERATION");
            var moderationOptions = OpenAiModerationOptions.builder().model("override-moderation").build();
            var moderationPrompt = new ModerationPrompt("审核", moderationOptions);
            var defaultModeration = new ModerationPrompt("默认审核");
            moderation.call(moderationPrompt);
            moderation.call(defaultModeration);
            assertThat(requests).allSatisfy(request -> assertThat(request.authorization()).isNull());
            assertDefaultOptionsUnchanged();
        }
    }

    private void callImageWithOptions(ModelClientRegistry registry) {
        var image = registry.getImageModel("tenant", "IMAGE");
        var options = OpenAiImageOptions.builder().model("override-image").width(1024).height(1024).build();
        var prompt = new ImagePrompt("图片", options);
        var defaultPrompt = new ImagePrompt("默认图片");
        image.call(prompt);
        image.call(defaultPrompt);
    }

    private void callSpeechWithOptions(ModelClientRegistry registry) {
        var speech = registry.getTextToSpeechModel("tenant", "TEXT_TO_SPEECH");
        var options = OpenAiAudioSpeechOptions.builder().model("override-speech").voice("nova").build();
        var prompt = new TextToSpeechPrompt("音频", options);
        var defaultPrompt = new TextToSpeechPrompt("默认音频");
        speech.call(prompt);
        speech.call(defaultPrompt);
    }

    private void callTranscriptionWithOptions(ModelClientRegistry registry) {
        var transcription = registry.getTranscriptionModel("tenant", "SPEECH_TO_TEXT");
        var options = OpenAiAudioTranscriptionOptions.builder().model("override-transcription").language("zh").build();
        var audio = audioResource();
        var prompt = new AudioTranscriptionPrompt(audio, options);
        var defaultPrompt = new AudioTranscriptionPrompt(audio);
        transcription.call(prompt);
        transcription.call(defaultPrompt);
    }

    private void assertDefaultOptionsUnchanged() throws Exception {
        assertThat(json.readTree(requests.get(0).body()).get("model").asText()).isEqualTo("override-image");
        assertThat(json.readTree(requests.get(1).body()).get("model").asText()).isEqualTo("test-IMAGE");
        assertThat(json.readTree(requests.get(2).body()).get("voice").asText()).isEqualTo("nova");
        assertThat(json.readTree(requests.get(3).body()).get("model").asText()).isEqualTo("test-TEXT_TO_SPEECH");
        assertThat(json.readTree(requests.get(3).body()).get("voice").asText()).isEqualTo("alloy");
        assertThat(requests.get(4).body()).contains("override-transcription");
        assertThat(requests.get(5).body()).contains("test-SPEECH_TO_TEXT").doesNotContain("override-transcription");
        assertThat(json.readTree(requests.get(6).body()).get("model").asText()).isEqualTo("override-moderation");
        assertThat(json.readTree(requests.get(7).body()).get("model").asText()).isEqualTo("test-MODERATION");
    }

    /** 协议能力必须明确白名单，新增枚举不能让其他协议错误进入向量分支。 */
    @Test
    void shouldRejectUnsupportedProtocolCapabilitiesBeforeResolvingCredentials() {
        var openai = new OpenAiModelClientProvider(transport, ObservationRegistry.NOOP);
        var dashscope = new DashScopeModelClientProvider(transport, ObservationRegistry.NOOP);
        var ollama = new OllamaModelClientProvider(transport, ObservationRegistry.NOOP);
        for (var capability : ModelCapability.values()) {
            assertThat(openai.supports(ModelClientProtocol.OPENAI_COMPATIBLE, capability))
                    .isEqualTo(capability != ModelCapability.RERANK);
            assertThat(dashscope.supports(ModelClientProtocol.DASHSCOPE, capability))
                    .isEqualTo(capability == ModelCapability.CHAT || capability == ModelCapability.EMBEDDING
                            || capability == ModelCapability.RERANK);
            assertThat(ollama.supports(ModelClientProtocol.OLLAMA, capability))
                    .isEqualTo(capability == ModelCapability.CHAT || capability == ModelCapability.EMBEDDING);
        }
        var root = URI.create("https://example.invalid/v1");
        var unsupported = new ModelConnectionConfiguration("tenant", "connection", 1, true, ModelClientProtocol.OLLAMA,
                ModelCapability.IMAGE, "test", root, () -> { throw new AssertionError("不支持的组合不得读取凭证"); });
        assertThatThrownBy(() -> factory.create(unsupported)).hasMessageContaining("没有支持");
        assertThatThrownBy(() -> ollama.create(unsupported)).hasMessageContaining("不支持");
        assertThatThrownBy(() -> dashscope.create(unsupported)).hasMessageContaining("不支持");
        assertThatThrownBy(() -> openai.create(unsupported)).hasMessageContaining("不支持");
        assertThat(requests).isEmpty();
    }

    /** 新能力仍执行单次请求策略，服务失败不能隐式重试付费调用。 */
    @ParameterizedTest
    @ValueSource(strings = {"IMAGE", "SPEECH_TO_TEXT", "TEXT_TO_SPEECH", "MODERATION"})
    void shouldNotRetryFailedCalls(String capabilityName) {
        responseStatus = 500;
        var capability = ModelCapability.valueOf(capabilityName);
        ModelConnectionResolver resolver = (tenant, id) -> configuration(tenant, capability, "/v1", null);
        try (var registry = new ModelClientRegistry(factory, resolver, properties)) {
            assertThatThrownBy(() -> callCapability(registry, capability)).isInstanceOf(RuntimeException.class);
            assertThat(requests).hasSize(1);
        }
    }

    private void callCapability(ModelClientRegistry registry, ModelCapability capability) {
        switch (capability) {
            case IMAGE -> {
                var prompt = new ImagePrompt("测试");
                registry.getImageModel("tenant", "IMAGE").call(prompt);
            }
            case SPEECH_TO_TEXT -> {
                var audio = audioResource();
                var prompt = new AudioTranscriptionPrompt(audio);
                registry.getTranscriptionModel("tenant", "SPEECH_TO_TEXT").call(prompt);
            }
            case TEXT_TO_SPEECH -> {
                var prompt = new TextToSpeechPrompt("测试");
                registry.getTextToSpeechModel("tenant", "TEXT_TO_SPEECH").call(prompt);
            }
            case MODERATION -> {
                var prompt = new ModerationPrompt("测试");
                registry.getModerationModel("tenant", "MODERATION").call(prompt);
            }
            default -> throw new IllegalArgumentException("非本次测试能力");
        }
    }

    private ModelConnectionConfiguration configuration(String tenant, ModelCapability capability, String apiRoot, String key) {
        var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + apiRoot);
        return new ModelConnectionConfiguration(tenant, capability.name(), 1, true, ModelClientProtocol.OPENAI_COMPATIBLE,
                capability, "test-" + capability.name(), uri, () -> key);
    }

    private ByteArrayResource audioResource() {
        var bytes = audioBytes();
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() { return "sample.wav"; }
        };
    }

    private byte[] audioBytes() { return "local-audio".getBytes(StandardCharsets.UTF_8); }

    private void respond(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        var request = new Request(exchange.getRequestMethod(), path, exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("Content-Type"), body);
        requests.add(request);
        byte[] response;
        if (responseStatus != 200) {
            response = "{\"error\":{\"message\":\"test failure\",\"type\":\"server_error\"}}".getBytes(StandardCharsets.UTF_8);
        } else if (path.endsWith("/audio/speech")) {
            response = audioBytes();
        } else {
            response = responseBody(path).getBytes(StandardCharsets.UTF_8);
        }
        String contentType = path.endsWith("/audio/speech") && responseStatus == 200 ? "audio/mpeg" : "application/json";
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(responseStatus, response.length);
        try (var output = exchange.getResponseBody()) { output.write(response); }
    }

    private String responseBody(String path) {
        if (path.endsWith("/images/generations")) {
            return "{\"created\":1,\"data\":[{\"url\":\"https://example.invalid/image.png\"}]}";
        }
        if (path.endsWith("/audio/transcriptions")) {
            return "{\"text\":\"测试转录\"}";
        }
        if (path.endsWith("/moderations")) {
            return "{\"id\":\"test\",\"model\":\"test-moderation\",\"results\":[{\"flagged\":false}]}";
        }
        throw new IllegalArgumentException("非预期SDK请求路径");
    }

    /** 本地合成请求快照，不含真实凭证。 */
    private record Request(String method, String path, String authorization, String contentType, String body) { }
}
