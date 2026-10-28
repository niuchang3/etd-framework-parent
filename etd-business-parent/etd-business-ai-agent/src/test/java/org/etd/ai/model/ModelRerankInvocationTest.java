package org.etd.ai.model;

import com.alibaba.cloud.ai.model.RerankModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.service.ModelInferenceService;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.model.client.ModelClientTransport;
import org.etd.framework.starter.ai.model.config.ModelClientProperties;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.etd.framework.starter.ai.model.provider.DashScopeModelClientProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.document.Document;
import com.alibaba.cloud.ai.document.DocumentWithScore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 贯通业务请求选项和原生SDK序列化，验证请求不会切回SDK内置的默认重排模型。 */
class ModelRerankInvocationTest {
    private final ObjectMapper json = new ObjectMapper();
    private HttpServer server;
    private ModelClientTransport transport;
    /** HTTP服务线程写入、测试线程读取，确保出站请求快照及时可见。 */
    private volatile JsonNode requestBody;

    /** 本地HTTP服务捕获实际出站请求，测试不调用真实服务商。 */
    @BeforeEach
    void prepare() throws IOException {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
        server = HttpServer.create(address, 0);
        server.createContext("/api/v1/services/rerank/text-rerank/text-rerank", this::respond);
        server.start();
        ModelClientProperties properties = new ModelClientProperties();
        transport = new ModelClientTransport(properties);
    }

    /** 原生模型和HTTP服务不跨测试共享。 */
    @AfterEach
    void close() {
        transport.close();
        server.stop(0);
    }

    /** 数量及返回正文选项只影响本次调用，不得覆盖业务连接配置的模型代码。 */
    @ParameterizedTest
    @ValueSource(strings = {"qwen3.7-text-rerank", "qwen3-rerank"})
    void preservesConfiguredModelWhenSettingRuntimeOptions(String modelCode) {
        ModelInferenceService inference = createInference(modelCode);
        Document document = Document.builder().id("101").text("测试候选正文").build();
        List<Document> documents = List.of(document);
        List<DocumentWithScore> result = inference.createRerankList(30L, "测试问题", documents);
        assertThat(requestBody.path("model").asText()).isEqualTo(modelCode);
        assertThat(requestBody.path("parameters").path("top_n").asInt()).isEqualTo(1);
        assertThat(requestBody.path("parameters").path("return_documents").asBoolean()).isFalse();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getOutput().getId()).isEqualTo("101");
    }

    /** SDK默认模型来自已授权连接，只有数据库Mapper和Registry查找在此替换。 */
    private ModelInferenceService createInference(String modelCode) {
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1");
        ModelConnectionConfiguration configuration = new ModelConnectionConfiguration("900001", "30", 1, true,
                ModelClientProtocol.DASHSCOPE, ModelCapability.RERANK, modelCode, endpoint, () -> null);
        DashScopeModelClientProvider provider = new DashScopeModelClientProvider(transport, ObservationRegistry.NOOP);
        RerankModel nativeModel = (RerankModel) provider.create(configuration);
        ModelClientRegistry clients = mock(ModelClientRegistry.class);
        when(clients.getRerankModel("900001", "30")).thenReturn(nativeModel);
        ModelConnectionMapper connections = mock(ModelConnectionMapper.class);
        ModelConnectionEntity connection = new ModelConnectionEntity();
        connection.setTenantId(900001L);
        when(connections.selectById(30L)).thenReturn(connection);
        return new ModelInferenceService(connections, clients);
    }

    /** 保留SDK按输入下标恢复原始文档的真实响应转换。 */
    private void respond(HttpExchange exchange) throws IOException {
        try (exchange) {
            requestBody = json.readTree(exchange.getRequestBody());
            String body = """
                    {"request_id":"test-request","output":{"results":[{"index":0,"relevance_score":0.9}]},
                     "usage":{"total_tokens":1}}
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
}
