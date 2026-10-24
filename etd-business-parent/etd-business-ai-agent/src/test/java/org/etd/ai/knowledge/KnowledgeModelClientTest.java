package org.etd.ai.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.etd.ai.knowledge.component.KnowledgeModelClient;
import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 使用本地HTTP替身校验真实协议载荷和响应约束，不调用外部收费模型。 */
class KnowledgeModelClientTest {
    /** OpenAI兼容服务按已选维度请求，不静默截断错误维度的模型结果。 */
    @Test
    void shouldSendSelectedDimensionsAndRejectWrongResponseSize() throws Exception {
        var address = new InetSocketAddress("127.0.0.1", 0);
        var server = HttpServer.create(address, 0);
        var json = new ObjectMapper();
        var captured = new AtomicReference<JsonNode>();
        var response = new AtomicReference<String>();
        var zeros = Collections.nCopies(768, 0.0);
        List<Double> values = new ArrayList<>(zeros);
        values.set(0, 1.0);
        var item = Map.of("embedding", values);
        var data = List.of(item);
        var body = Map.of("data", data);
        response.set(json.writeValueAsString(body));
        server.createContext("/embeddings", exchange -> {
            captured.set(json.readTree(exchange.getRequestBody()));
            byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
        try {
            var connections = mock(ModelConnectionMapper.class);
            var models = mock(ModelMapper.class);
            var cipher = mock(ModelCredentialCipher.class);
            var connection = new ModelConnectionEntity();
            connection.setId(1L);
            connection.setModelId(2L);
            connection.setDataStatus(1);
            connection.setProtocol("openai_compatible");
            connection.setAuthType("none");
            connection.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            when(connections.selectById(1L)).thenReturn(connection);
            var model = new ModelEntity();
            model.setModelCode("test-embedding");
            model.setModelType("embedding");
            model.setDataStatus(1);
            Integer[] dimensions = {768, 1024};
            model.setEmbedding(dimensions);
            when(models.selectById(2L)).thenReturn(model);
            var client = new KnowledgeModelClient(connections, models, cipher);
            var embedding = client.embed(1L, 768, "安装指南", false);
            assertThat(embedding).hasSize(768);
            assertThat(captured.get().path("dimensions").asInt()).isEqualTo(768);
            assertThat(captured.get().path("input").asText()).isEqualTo("安装指南");
            response.set("{\"data\":[{\"embedding\":[1,0]}]}");
            assertThatThrownBy(() -> client.embed(1L, 768, "安装指南", false))
                    .isInstanceOf(ApiRuntimeException.class).hasMessageContaining("维度");
        } finally {
            server.stop(0);
        }
    }
}
