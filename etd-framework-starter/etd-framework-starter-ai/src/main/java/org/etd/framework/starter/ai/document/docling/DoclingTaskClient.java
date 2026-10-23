package org.etd.framework.starter.ai.document.docling;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.ChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.etd.framework.starter.ai.document.model.DocumentParseAdvancedOptions;
import org.etd.framework.starter.ai.document.model.DocumentChunkOptions;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
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
 * 复用官方SDK的传输、认证及结果API，补充分块原生字段和提交后立即返回任务的入口。
 * SDK 的 convertSourceAsync 会自动轮询，无法把任务交给业务持久化管理，因此不直接使用。
 */
public final class DoclingTaskClient extends DoclingServeClient {
    /** 官方 v1 的任务结果路径。 */
    private static final String RESULT_PATH = "/v1/result/";
    /** 官方异步提交端点的固定后缀。 */
    private static final String ASYNC_PATH_SUFFIX = "/async";
    /** 官方 v1 的单文档异步提交路径。 */
    private static final String SUBMIT_PATH = "/v1/convert/source" + ASYNC_PATH_SUFFIX;
    /** 官方v1层级分块基础路径，同步与异步共用。 */
    private static final String HIERARCHICAL_CHUNK_PATH = "/v1/chunk/hierarchical/source";
    /** 官方v1混合分块基础路径，同步与异步共用。 */
    private static final String HYBRID_CHUNK_PATH = "/v1/chunk/hybrid/source";
    /** 分块请求中的原文解析选项节点。 */
    private static final String CONVERT_OPTIONS_FIELD = "convert_options";
    /** 普通解析请求中的选项节点。 */
    private static final String OPTIONS_FIELD = "options";
    /** 分块请求中的切分选项节点。 */
    private static final String CHUNKING_OPTIONS_FIELD = "chunking_options";
    /** 原生图片序列化开关，官方SDK当前版本尚未声明。 */
    private static final String MARKDOWN_IMAGES_FIELD = "use_markdown_images";
    /** 原生图片占位文本，官方SDK当前版本尚未声明。 */
    private static final String IMAGE_PLACEHOLDER_FIELD = "image_placeholder";
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
     * @param advanced 高级解析选项，不填使用默认值
     * @return 服务端任务快照，不启动后台轮询
     */
    public TaskStatusPollResponse submit(ConvertDocumentRequest request, DocumentParseAdvancedOptions advanced) {
        JsonNode payload = createPayload(request, advanced, null);
        var context = RequestContext.<JsonNode, TaskStatusPollResponse>builder()
                .uri(SUBMIT_PATH).request(payload).responseType(TaskStatusPollResponse.class).build();
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
     * @param request SDK原生分块请求
     * @param advanced 高级解析选项，不填使用默认值
     * @param options 当前分块选项，包含图片序列化设置
     * @return 同步分块结果，执行期间不启动轮询
     */
    public ChunkDocumentResponse chunk(ChunkDocumentRequest request, DocumentParseAdvancedOptions advanced,
            DocumentChunkOptions options) {
        String path = chunkPath(options.strategy(), false);
        JsonNode payload = createPayload(request, advanced, options);
        var context = RequestContext.<JsonNode, ChunkDocumentResponse>builder()
                .uri(path).request(payload).responseType(ChunkDocumentResponse.class).build();
        return executePost(context);
    }

    /**
     * @param request SDK原生分块请求
     * @param advanced 高级解析选项，不填使用默认值
     * @param options 当前分块选项，包含图片序列化设置
     * @return 任务快照，不启动后台轮询
     */
    public TaskStatusPollResponse submitChunk(ChunkDocumentRequest request, DocumentParseAdvancedOptions advanced,
            DocumentChunkOptions options) {
        String path = chunkPath(options.strategy(), true);
        JsonNode payload = createPayload(request, advanced, options);
        var context = RequestContext.<JsonNode, TaskStatusPollResponse>builder()
                .uri(path).request(payload).responseType(TaskStatusPollResponse.class).build();
        return executePost(context);
    }

    /** 同一策略的同步和异步请求共享payload，解析及切分全部交给Docling。 */
    private String chunkPath(DocumentChunkStrategy strategy, boolean async) {
        String path = switch (strategy) {
            case HIERARCHICAL -> HIERARCHICAL_CHUNK_PATH;
            case HYBRID -> HYBRID_CHUNK_PATH;
            case REGEX -> throw new IllegalArgumentException("Docling原生接口不支持正则分片");
        };
        return async ? path + ASYNC_PATH_SUFFIX : path;
    }

    /**
     * 补齐SDK尚未声明的官方字段；仍复用SDK的HTTP、认证和错误处理。
     * 选项来自明确类型，不允许覆盖文件来源、目标或原始任务路径。
     */
    private JsonNode createPayload(Object request, DocumentParseAdvancedOptions advanced, DocumentChunkOptions chunk) {
        ObjectNode payload = mapper.valueToTree(request);
        String optionsField = chunk == null ? OPTIONS_FIELD : CONVERT_OPTIONS_FIELD;
        ObjectNode conversion = (ObjectNode) payload.get(optionsField);
        if (advanced != null) {
            ObjectNode additional = mapper.valueToTree(advanced);
            conversion.setAll(additional);
        }
        if (chunk != null) {
            applyImageOptions(payload, chunk);
        }
        return payload;
    }

    private void applyImageOptions(ObjectNode payload, DocumentChunkOptions options) {
        ObjectNode chunk = (ObjectNode) payload.get(CHUNKING_OPTIONS_FIELD);
        if (options.useMarkdownImages() != null) {
            chunk.put(MARKDOWN_IMAGES_FIELD, options.useMarkdownImages());
        }
        if (options.imagePlaceholder() != null) {
            chunk.put(IMAGE_PLACEHOLDER_FIELD, options.imagePlaceholder());
        }
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
