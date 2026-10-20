package org.etd.ai.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.etd.ai.knowledge.service.KnowledgeEmbeddingService;
import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.model.config.ModelCredentialProperties;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** 使用本地HTTP服务核对三种向量协议的真实请求结构，不调用外部付费模型。 */
class KnowledgeEmbeddingProtocolTest {
    /** 各协议按官方API根路径调用，并显式传递资料选择的向量维度。 */
    @ParameterizedTest
    @ValueSource(strings = {"dashscope", "ollama", "openai_compatible"})
    void shouldUseSelectedProtocolAndDimensions(String protocol) throws Exception {
        var json = new ObjectMapper();
        var captured = new AtomicReference<JsonNode>();
        var address = new InetSocketAddress("127.0.0.1", 0);
        var server = HttpServer.create(address, 0);
        String path = switch (protocol) {
            case "dashscope" -> "/api/v1/services/embeddings/text-embedding/text-embedding";
            case "ollama" -> "/api/embed";
            default -> "/embeddings";
        };
        byte[] response = createResponse(json, protocol, 768);
        server.createContext(path, exchange -> {
            captured.set(json.readTree(exchange.getRequestBody()));
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var connection = createConnection(protocol, server.getAddress().getPort());
            var properties = new ModelCredentialProperties();
            var cipher = new ModelCredentialCipher(properties);
            var service = new KnowledgeEmbeddingService(json, cipher);
            var model = new ModelEntity();
            model.setModelCode("test-embedding");
            assertEquals(768, service.createEmbedding(connection, model, 768, "正文").size());
            var request = captured.get();
            assertEquals("test-embedding", request.path("model").asText());
            if ("dashscope".equals(protocol)) {
                assertEquals(768, request.path("parameters").path("dimension").asInt());
                assertEquals("document", request.path("parameters").path("text_type").asText());
            } else {
                assertEquals(768, request.path("dimensions").asInt());
            }
            if ("ollama".equals(protocol)) {
                assertFalse(request.path("truncate").asBoolean());
            }
        } finally {
            server.stop(0);
        }
    }

    /** 模型忽略维度参数时拒绝写入，不能截断或补零制造错误向量。 */
    @Test
    void shouldRejectUnexpectedVectorDimensions() throws Exception {
        var json = new ObjectMapper();
        var address = new InetSocketAddress("127.0.0.1", 0);
        var server = HttpServer.create(address, 0);
        byte[] response = createResponse(json, "openai_compatible", 2);
        server.createContext("/embeddings", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var connection = createConnection("openai_compatible", server.getAddress().getPort());
            var properties = new ModelCredentialProperties();
            var cipher = new ModelCredentialCipher(properties);
            var service = new KnowledgeEmbeddingService(json, cipher);
            var model = new ModelEntity();
            model.setModelCode("test-embedding");
            assertThrows(ApiRuntimeException.class, () -> service.createEmbedding(connection, model, 768, "正文"));
        } finally {
            server.stop(0);
        }
    }

    /** 创建无认证的本地协议连接；DashScope根地址按协议包含/api/v1。 */
    private ModelConnectionEntity createConnection(String protocol, int port) {
        var connection = new ModelConnectionEntity();
        connection.setProtocol(protocol);
        connection.setAuthType("none");
        String root = "http://127.0.0.1:" + port;
        connection.setBaseUrl("dashscope".equals(protocol) ? root + "/api/v1" : root);
        return connection;
    }

    /** 按各协议生成确定维度的模拟响应。 */
    private byte[] createResponse(ObjectMapper json, String protocol, int dimension) throws Exception {
        var values = new ArrayList<Double>();
        for (int index = 0; index < dimension; index++) {
            values.add(index == 0 ? 1.0 : 0.0);
        }
        var embedding = Map.of("embedding", values);
        var rows = List.of(embedding);
        var output = Map.of("embeddings", rows);
        var arrays = List.of(values);
        Object response = switch (protocol) {
            case "dashscope" -> Map.of("output", output);
            case "ollama" -> Map.of("embeddings", arrays);
            default -> Map.of("data", rows);
        };
        return json.writeValueAsBytes(response);
    }
}
