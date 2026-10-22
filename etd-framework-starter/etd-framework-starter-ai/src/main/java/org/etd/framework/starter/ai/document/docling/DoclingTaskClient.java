package org.etd.framework.starter.ai.document.docling;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import ai.docling.serve.client.DoclingServeClient;
import ai.docling.serve.client.JsonReadException;
import ai.docling.serve.client.operations.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * 复用官方 SDK 的传输、认证及结果 API，只补充“提交后立即返回任务”的入口。
 * SDK 的 convertSourceAsync 会自动轮询，无法把任务交给业务持久化管理，因此不直接使用。
 */
public final class DoclingTaskClient extends DoclingServeClient {
    /** 官方 v1 的任务结果路径。 */
    private static final String RESULT_PATH = "/v1/result/";
    /** 官方 v1 的单文档异步提交路径。 */
    private static final String SUBMIT_PATH = "/v1/convert/source/async";
    /** 官方 v1 的层级分块异步提交路径。 */
    private static final String HIERARCHICAL_CHUNK_SUBMIT_PATH = "/v1/chunk/hierarchical/source/async";
    /** 官方 v1 的混合分块异步提交路径。 */
    private static final String HYBRID_CHUNK_SUBMIT_PATH = "/v1/chunk/hybrid/source/async";
    /** 用于识别分块结果形状，防止把普通解析任务结果误当作空分块结果。 */
    private static final String CHUNKS_FIELD = "chunks";
    /** 原文结果数组，即使未请求返回原文也应由分块接口提供空数组。 */
    private static final String DOCUMENTS_FIELD = "documents";
    /** 独立且不可变的技术 JSON 配置，不复用业务多态序列化设置。 */
    private final JsonMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    private DoclingTaskClient(Builder builder) {
        super(builder);
    }

    /** @return 使用官方 SDK 连接管理的客户端构建器 */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param request 单文件、内联结果的官方请求
     * @return 服务端任务快照，不启动后台轮询
     */
    public TaskStatusPollResponse submit(ConvertDocumentRequest request) {
        var context = RequestContext.<ConvertDocumentRequest, TaskStatusPollResponse>builder()
                .uri(SUBMIT_PATH).request(request).responseType(TaskStatusPollResponse.class).build();
        return executePost(context);
    }

    @Override
    protected <T> T readValue(String json, Class<T> type) {
        try {
            if (type == ChunkDocumentResponse.class) {
                JsonNode tree = mapper.readTree(json);
                if (tree == null || !tree.path(CHUNKS_FIELD).isArray() || !tree.path(DOCUMENTS_FIELD).isArray()) {
                    throw new IllegalArgumentException("解析引擎未返回分块结果，可能使用了错误的任务类型");
                }
                return mapper.treeToValue(tree, type);
            }
            return mapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new JsonReadException(exception);
        }
    }

    @Override
    protected <T> String writeValueAsString(T value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Docling请求序列化失败", exception);
        }
    }

    /**
     * 获取本适配器约定的内联 JSON；避免 SDK 通用结果方法返回未消费的 ZIP 流。
     * @param taskId 已校验的任务标识
     * @return 单份文档结果
     */
    public InBodyConvertDocumentResponse fetchInlineResult(String taskId) {
        var context = RequestContext.<Object, InBodyConvertDocumentResponse>builder()
                .uri(RESULT_PATH + taskId).responseType(InBodyConvertDocumentResponse.class).build();
        return executeGet(context);
    }

    /**
     * @param request 层级分块 SDK 请求
     * @return 任务快照，不启动后台轮询
     */
    public TaskStatusPollResponse submitChunk(HierarchicalChunkDocumentRequest request) {
        var context = RequestContext.<HierarchicalChunkDocumentRequest, TaskStatusPollResponse>builder()
                .uri(HIERARCHICAL_CHUNK_SUBMIT_PATH).request(request).responseType(TaskStatusPollResponse.class).build();
        return executePost(context);
    }

    /**
     * @param request 混合分块 SDK 请求
     * @return 任务快照，不启动后台轮询
     */
    public TaskStatusPollResponse submitChunk(HybridChunkDocumentRequest request) {
        var context = RequestContext.<HybridChunkDocumentRequest, TaskStatusPollResponse>builder()
                .uri(HYBRID_CHUNK_SUBMIT_PATH).request(request).responseType(TaskStatusPollResponse.class).build();
        return executePost(context);
    }

    /**
     * @param taskId 已校验的分块任务标识
     * @return 单文档分块结果，响应类型不符时明确失败
     */
    public ChunkDocumentResponse fetchInlineChunkResult(String taskId) {
        var context = RequestContext.<Object, ChunkDocumentResponse>builder()
                .uri(RESULT_PATH + taskId).responseType(ChunkDocumentResponse.class).build();
        return executeGet(context);
    }

    /**
     * 官方 SDK 基类仍强制实现此方法；仅在这个覆盖点抑制上游废弃与泛型返回警告。
     * @return 保留服务配置的构建器；客户端使用期间无需重新构建
     */
    @Override
    @SuppressWarnings({"removal", "unchecked"})
    public <T extends DoclingServeApi, B extends DoclingApiBuilder<T, B>> DoclingApiBuilder<T, B> toBuilder() {
        return (DoclingApiBuilder<T, B>) new Builder(this);
    }

    /** SDK 要求的具体构建器；每个解析器仅构建一次，不按任务创建客户端。 */
    public static final class Builder extends DoclingServeClientBuilder<DoclingTaskClient, Builder> {
        private Builder() {
        }

        private Builder(DoclingTaskClient client) {
            super(client);
        }

        /** @return 包含可复用 HTTP 连接管理的客户端 */
        @Override
        public DoclingTaskClient build() {
            return new DoclingTaskClient(this);
        }
    }
}
