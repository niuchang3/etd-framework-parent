package org.etd.ai.knowledge.component;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.model.constant.ModelProtocol;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

/** 租户模型调用边界：凭证只在请求时解密，错误正文不传递到接口或日志。 */
@Component
@RequiredArgsConstructor
public class KnowledgeModelClient {
    /** 模型协议固定字段。 */
    private static final String MODEL = "model", INPUT = "input", DIMENSIONS = "dimensions", PARAMETERS = "parameters",
            TEXTS = "texts", TEXT_TYPE = "text_type", DATA = "data", OUTPUT = "output", EMBEDDINGS = "embeddings",
            EMBEDDING = "embedding", QUERY = "query", DOCUMENTS = "documents", RESULTS = "results",
            INDEX = "index", SCORE = "relevance_score", TOP_N = "top_n", TRUNCATE = "truncate";
    private final ModelConnectionMapper connections;
    private final ModelMapper models;
    private final ModelCredentialCipher cipher;
    /** 复用无租户状态的HTTP连接池，认证只设置在单次请求上。 */
    private final RestClient client = createClient();

    private static RestClient createClient() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder().requestFactory(factory).build();
    }

    /** @param connectionId 连接 @param type 能力类型 @param dimensions 可选维度；校验启停及模型能力 */
    public void validate(Long connectionId, String type, Integer dimensions) {
        var connection = requireConnection(connectionId);
        var model = requireModel(connection, type);
        if (dimensions != null && (model.getEmbedding() == null || !Arrays.asList(model.getEmbedding()).contains(dimensions))) {
            throw new ApiRuntimeException("所选维度不在模型支持范围内");
        }
        if (ModelType.RERANK.equals(type) && !ModelProtocol.DASHSCOPE.equals(connection.getProtocol())) {
            throw new ApiRuntimeException("当前重排调用支持DashScope原生协议，请选择对应连接");
        }
    }

    /** @param connectionId 向量连接 @param dimensions 固定维度 @param text 正文或问题 @param query 是否查询向量 @return 有限且非零的向量 */
    public float[] embed(Long connectionId, int dimensions, String text, boolean query) {
        var connection = requireConnection(connectionId);
        var model = requireModel(connection, ModelType.EMBEDDING);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put(MODEL, model.getModelCode());
        if (model.getEmbedding() != null && model.getEmbedding().length > 1) {
            request.put(DIMENSIONS, dimensions);
        }
        String endpoint;
        switch (connection.getProtocol()) {
            case ModelProtocol.DASHSCOPE -> {
                var texts = List.of(text);
                var input = Map.of(TEXTS, texts);
                Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put(TEXT_TYPE, query ? QUERY : "document");
                if (model.getEmbedding() != null && model.getEmbedding().length > 1) {
                    parameters.put(DIMENSIONS, dimensions);
                }
                request.remove(DIMENSIONS);
                request.put(INPUT, input);
                request.put(PARAMETERS, parameters);
                endpoint = "/services/embeddings/text-embedding/text-embedding";
            }
            case ModelProtocol.OLLAMA -> {
                request.put(INPUT, text);
                request.put(TRUNCATE, false);
                endpoint = "/api/embed";
            }
            case ModelProtocol.OPENAI_COMPATIBLE -> {
                request.put(INPUT, text);
                endpoint = "/embeddings";
            }
            default -> throw new ApiRuntimeException("不支持的向量协议");
        }
        var response = post(connection, endpoint, request);
        JsonNode values = switch (connection.getProtocol()) {
            case ModelProtocol.DASHSCOPE -> response.path(OUTPUT).path(EMBEDDINGS).path(0).path(EMBEDDING);
            case ModelProtocol.OLLAMA -> response.path(EMBEDDINGS).path(0);
            default -> response.path(DATA).path(0).path(EMBEDDING);
        };
        return requireVector(values, dimensions);
    }

    /** @param connectionId 重排连接 @param query 查询 @param documents 候选正文 @return 下标到原始重排分数 */
    public Map<Integer, Double> rerank(Long connectionId, String query, List<String> documents) {
        validate(connectionId, ModelType.RERANK, null);
        var connection = requireConnection(connectionId);
        var model = requireModel(connection, ModelType.RERANK);
        var input = Map.of(QUERY, query, DOCUMENTS, documents);
        var parameters = Map.of(TOP_N, documents.size());
        var request = Map.of(MODEL, model.getModelCode(), INPUT, input, PARAMETERS, parameters);
        var response = post(connection, "/services/rerank/text-rerank/text-rerank", request);
        Map<Integer, Double> scores = new HashMap<>();
        for (var result : response.path(OUTPUT).path(RESULTS)) {
            int index = result.path(INDEX).asInt(-1);
            double score = result.path(SCORE).asDouble(Double.NaN);
            if (index < 0 || index >= documents.size() || !Double.isFinite(score) || scores.putIfAbsent(index, score) != null) {
                throw new ApiRuntimeException("重排响应格式不正确");
            }
        }
        if (scores.size() != documents.size()) {
            throw new ApiRuntimeException("重排模型未返回全部候选评分");
        }
        return scores;
    }

    /** 固定超时，屏蔽远端响应正文，API根地址由租户连接显式维护。 */
    private JsonNode post(ModelConnectionEntity connection, String endpoint, Object request) {
        String url = connection.getBaseUrl().replaceAll("/+$", "") + endpoint;
        var spec = client.post().uri(url).contentType(MediaType.APPLICATION_JSON);
        if (ModelProtocol.AUTH_API_KEY.equals(connection.getAuthType())) {
            String key = cipher.decrypt(connection.getApiKeyCiphertext(), connection.getCredentialKeyId(),
                    connection.getTenantId(), connection.getId());
            spec.headers(headers -> headers.setBearerAuth(key));
        }
        try {
            var response = spec.body(request).retrieve().body(JsonNode.class);
            if (response == null) {
                throw new ApiRuntimeException("模型返回为空");
            }
            return response;
        } catch (RuntimeException exception) {
            throw new ApiRuntimeException("模型调用失败，请检查连接、配额和模型参数");
        }
    }

    private ModelConnectionEntity requireConnection(Long id) {
        var connection = id == null ? null : connections.selectById(id);
        if (connection == null || !Objects.equals(connection.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("模型连接不存在或已停用");
        }
        return connection;
    }

    private ModelEntity requireModel(ModelConnectionEntity connection, String type) {
        var model = models.selectById(connection.getModelId());
        if (model == null || !type.equals(model.getModelType())
                || !Objects.equals(model.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("模型不存在、已停用或类型不匹配");
        }
        return model;
    }

    private float[] requireVector(JsonNode values, int dimensions) {
        if (!values.isArray() || values.size() != dimensions) {
            throw new ApiRuntimeException("模型实际返回维度与配置不一致");
        }
        float[] result = new float[dimensions];
        double norm = 0;
        for (int index = 0; index < dimensions; index++) {
            var node = values.get(index);
            result[index] = (float) node.asDouble(Double.NaN);
            if (!node.isNumber() || !Float.isFinite(result[index])) {
                throw new ApiRuntimeException("模型返回无效向量");
            }
            norm += (double) result[index] * result[index];
        }
        if (norm == 0) {
            throw new ApiRuntimeException("模型返回零向量");
        }
        return result;
    }
}
