package org.etd.ai.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.model.constant.ModelProtocol;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** 按资料选择的连接器调用向量模型，不创建绑定全局密钥的客户端。 */
@Service
@RequiredArgsConstructor
public class KnowledgeEmbeddingService {
    /** 模型调用代码字段。 */
    private static final String FIELD_MODEL = "model";
    /** 模型输入字段。 */
    private static final String FIELD_INPUT = "input";
    /** 兼容协议与Ollama的输出维度字段。 */
    private static final String FIELD_DIMENSIONS = "dimensions";
    /** DashScope的输出维度字段。 */
    private static final String FIELD_DIMENSION = "dimension";
    /** DashScope参数对象字段。 */
    private static final String FIELD_PARAMETERS = "parameters";
    /** DashScope正文数组字段。 */
    private static final String FIELD_TEXTS = "texts";
    /** DashScope正文角色字段。 */
    private static final String FIELD_TEXT_TYPE = "text_type";
    /** Ollama是否自动截断正文的字段。 */
    private static final String FIELD_TRUNCATE = "truncate";
    /** 单条向量字段。 */
    private static final String FIELD_EMBEDDING = "embedding";
    /** 向量集合字段。 */
    private static final String FIELD_EMBEDDINGS = "embeddings";
    /** DashScope输出对象字段。 */
    private static final String FIELD_OUTPUT = "output";
    /** 兼容协议数据集合字段。 */
    private static final String FIELD_DATA = "data";
    /** 不自动重定向，避免认证头被发送到其他地址。 */
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ObjectMapper json;
    private final ModelCredentialCipher cipher;

    /** @param connection 当前可用连接 @param model 平台模型 @param dimension 输出维度 @param text 待向量化正文 @return 有限浮点向量 */
    public List<Double> createEmbedding(ModelConnectionEntity connection, ModelEntity model, int dimension, String text) {
        try {
            String endpoint = resolveEndpoint(connection);
            var payload = createPayload(connection.getProtocol(), model.getModelCode(), dimension, text);
            String body = json.writeValueAsString(payload);
            var uri = URI.create(endpoint);
            var builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            if (ModelProtocol.AUTH_API_KEY.equals(connection.getAuthType())) {
                String key = cipher.decrypt(connection.getApiKeyCiphertext(), connection.getCredentialKeyId(),
                        connection.getTenantId(), connection.getId());
                builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + key);
            }
            var publisher = HttpRequest.BodyPublishers.ofString(body);
            var request = builder.POST(publisher).build();
            var handler = HttpResponse.BodyHandlers.ofInputStream();
            var response = http.send(request, handler);
            try (var input = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new ApiRuntimeException("向量模型请求失败，HTTP状态=" + response.statusCode());
                }
                byte[] bytes = input.readNBytes(1024 * 1024 + 1);
                if (bytes.length > 1024 * 1024) {
                    throw new ApiRuntimeException("向量模型响应过大");
                }
                JsonNode result = json.readTree(bytes);
                return readEmbedding(connection.getProtocol(), result, dimension);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiRuntimeException("向量模型调用被中断");
        } catch (ApiRuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiRuntimeException("向量模型调用失败，请检查连接器与模型维度");
        }
    }

    /** 协议分支固定；DashScope根地址包含/api/v1，OpenAI兼容根地址包含其API版本路径。 */
    private String resolveEndpoint(ModelConnectionEntity connection) {
        String root = connection.getBaseUrl().replaceAll("/+$", "");
        return switch (connection.getProtocol()) {
            case ModelProtocol.OLLAMA -> root + "/api/embed";
            case ModelProtocol.DASHSCOPE -> root + "/services/embeddings/text-embedding/text-embedding";
            case ModelProtocol.OPENAI_COMPATIBLE -> root + "/embeddings";
            default -> throw new ApiRuntimeException("不支持的向量接入协议");
        };
    }

    /** 各协议均显式传递资料选择的维度，输出必须严格匹配。 */
    private Map<String, Object> createPayload(String protocol, String model, int dimension, String text) {
        if (ModelProtocol.DASHSCOPE.equals(protocol)) {
            var contents = List.of(text);
            var input = Map.of(FIELD_TEXTS, contents);
            var parameters = Map.of(FIELD_DIMENSION, dimension, FIELD_TEXT_TYPE, "document");
            return Map.of(FIELD_MODEL, model, FIELD_INPUT, input, FIELD_PARAMETERS, parameters);
        }
        if (ModelProtocol.OLLAMA.equals(protocol)) {
            return Map.of(FIELD_MODEL, model, FIELD_INPUT, text, FIELD_DIMENSIONS, dimension, FIELD_TRUNCATE, false);
        }
        return Map.of(FIELD_MODEL, model, FIELD_INPUT, text, FIELD_DIMENSIONS, dimension);
    }

    /** 校验服务商响应及向量维度，不把异常响应正文泄露到客户端。 */
    private List<Double> readEmbedding(String protocol, JsonNode result, int dimension) {
        JsonNode array = switch (protocol) {
            case ModelProtocol.DASHSCOPE -> result.path(FIELD_OUTPUT).path(FIELD_EMBEDDINGS).path(0).path(FIELD_EMBEDDING);
            case ModelProtocol.OLLAMA -> result.path(FIELD_EMBEDDINGS).path(0);
            default -> result.path(FIELD_DATA).path(0).path(FIELD_EMBEDDING);
        };
        if (!array.isArray() || array.size() != dimension) {
            throw new ApiRuntimeException("模型实际输出维度与资料配置不一致");
        }
        var values = new ArrayList<Double>();
        for (JsonNode item : array) {
            if (!item.isNumber() || !Double.isFinite(item.asDouble())) {
                throw new ApiRuntimeException("模型返回了无效向量");
            }
            values.add(item.asDouble());
        }
        return values;
    }
}
