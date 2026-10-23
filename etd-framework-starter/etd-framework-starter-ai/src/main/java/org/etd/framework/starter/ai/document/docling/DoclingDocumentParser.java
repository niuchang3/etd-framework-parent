package org.etd.framework.starter.ai.document.docling;

import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.chunk.request.ChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.ImageRefMode;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.request.source.FileSource;
import ai.docling.serve.api.convert.request.target.InBodyTarget;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;
import ai.docling.serve.api.task.request.TaskStatusPollRequest;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import ai.docling.serve.client.DoclingServeClientException;
import ai.docling.serve.api.validation.ValidationException;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.etd.framework.starter.ai.document.docling.constant.DoclingFileFormat;
import org.etd.framework.starter.ai.document.constant.DocumentParseStatus;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.docling.converter.DoclingChunkConverter;
import org.etd.framework.starter.ai.document.model.DocumentChunkRequest;
import org.etd.framework.starter.ai.document.model.DocumentChunkResult;
import org.etd.framework.starter.ai.document.model.HierarchicalDocumentChunkOptions;
import org.etd.framework.starter.ai.document.model.HybridDocumentChunkOptions;
import org.etd.framework.starter.ai.document.parser.DocumentChunker;
import org.mapstruct.factory.Mappers;
import org.etd.framework.starter.ai.document.docling.converter.DoclingContent;
import org.etd.framework.starter.ai.document.docling.converter.DoclingResultConverter;
import org.etd.framework.starter.ai.document.docling.converter.DoclingStructureReader;
import org.etd.framework.starter.ai.document.exception.DocumentParseException;
import org.etd.framework.starter.ai.document.model.DocumentParseOptions;
import org.etd.framework.starter.ai.document.model.DocumentParseRequest;
import org.etd.framework.starter.ai.document.model.DocumentParseResult;
import org.etd.framework.starter.ai.document.model.DocumentParseTask;
import org.etd.framework.starter.ai.document.model.DocumentParserCapabilities;
import org.etd.framework.starter.ai.document.parser.AsynchronousDocumentParser;
import org.springframework.util.Assert;
import org.springframework.http.HttpStatus;

/** Docling 单文档异步适配，不持久化任务、不自动轮询、不携带租户或业务实体。 */
public final class DoclingDocumentParser implements AsynchronousDocumentParser, DocumentChunker {
    /** 稳定的 Docling 引擎代码。 */
    public static final String ENGINE = "docling";
    /** Docling任务标识的长度上限，限制其作为URL路径片段的大小。 */
    private static final int MAX_TASK_ID_LENGTH = 200;
    /** 仅允许适合作为路径片段的任务标识字符，长度单独校验。 */
    private static final Pattern TASK_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    /** 当前适配器暴露的原生分块策略，不对未实现策略提供兜底实现。 */
    private static final Set<DocumentChunkStrategy> CHUNK_STRATEGIES = Set.of(
            DocumentChunkStrategy.HIERARCHICAL, DocumentChunkStrategy.HYBRID);
    private final DoclingChunkConverter chunkConverter = Mappers.getMapper(DoclingChunkConverter.class);
    private final DoclingTaskClient client;
    private final DoclingResultConverter converter;
    private final int maxFileBytes;
    private final DoclingStructureReader structureReader = new DoclingStructureReader();

    /**
     * @param client 复用的 SDK 客户端，不能在调用间修改连接或认证配置
     * @param converter SDK 到公共契约的转换器
     * @param maxFileBytes 单份原文件最大字节数
     * @throws IllegalArgumentException 依赖或大小限制无效
     */
    public DoclingDocumentParser(DoclingTaskClient client, DoclingResultConverter converter, int maxFileBytes) {
        Assert.notNull(client, "Docling客户端不能为空");
        Assert.notNull(converter, "解析结果转换器不能为空");
        Assert.isTrue(maxFileBytes > 0 && maxFileBytes < Integer.MAX_VALUE, "文件大小限制无效");
        this.client = client;
        this.converter = converter;
        this.maxFileBytes = maxFileBytes;
    }

    @Override
    public String engine() {
        return ENGINE;
    }

    @Override
    public DocumentParserCapabilities capabilities() {
        var extensions = DoclingFileFormat.supportedExtensions();
        return new DocumentParserCapabilities(extensions, true, true, true, CHUNK_STRATEGIES);
    }

    @Override
    public DocumentParseTask submit(DocumentParseRequest request) {
        Assert.notNull(request, "解析请求不能为空");
        validateFormat(request);
        ConvertDocumentRequest sdkRequest = createRequest(request);
        Supplier<TaskStatusPollResponse> operation = () -> client.submit(sdkRequest, request.options().advanced());
        TaskStatusPollResponse response = invoke(operation);
        return requireTask(response, null);
    }

    @Override
    public DocumentParseTask fetchStatus(String taskId) {
        validateTaskId(taskId);
        var request = TaskStatusPollRequest.builder().taskId(taskId).build();
        Supplier<TaskStatusPollResponse> operation = () -> client.pollTaskStatus(request);
        TaskStatusPollResponse response = invoke(operation);
        return requireTask(response, taskId);
    }

    @Override
    public DocumentParseResult fetchResult(String taskId) {
        validateTaskId(taskId);
        Supplier<InBodyConvertDocumentResponse> operation = () -> client.fetchInlineResult(taskId);
        InBodyConvertDocumentResponse result = invoke(operation);
        if (result == null) {
            throw new DocumentParseException("解析引擎未返回单份文档内联结果", DocumentParseException.NO_HTTP_STATUS, null);
        }
        return convertResult(result);
    }

    @Override
    public DocumentChunkResult chunk(DocumentChunkRequest request) {
        ChunkDocumentRequest sdkRequest = createChunkRequest(request);
        Supplier<ChunkDocumentResponse> operation = () -> client.chunk(sdkRequest,
                request.document().options().advanced(), request.options());
        ChunkDocumentResponse response = invoke(operation);
        return convertChunkResult(response);
    }

    @Override
    public DocumentParseTask submitChunk(DocumentChunkRequest request) {
        ChunkDocumentRequest sdkRequest = createChunkRequest(request);
        Supplier<TaskStatusPollResponse> operation = () -> client.submitChunk(sdkRequest,
                request.document().options().advanced(), request.options());
        TaskStatusPollResponse response = invoke(operation);
        return requireTask(response, null);
    }

    @Override
    public DocumentChunkResult fetchChunkResult(String taskId) {
        validateTaskId(taskId);
        Supplier<ChunkDocumentResponse> operation = () -> client.fetchInlineChunkResult(taskId);
        ChunkDocumentResponse response = invoke(operation);
        return convertChunkResult(response);
    }

    private ChunkDocumentRequest createChunkRequest(DocumentChunkRequest request) {
        Assert.notNull(request, "分块请求不能为空");
        Assert.isTrue(CHUNK_STRATEGIES.contains(request.options().strategy()), "解析引擎不支持该分块策略");
        validateFormat(request.document());
        ConvertDocumentRequest conversion = createRequest(request.document());
        return switch (request.options()) {
            case HierarchicalDocumentChunkOptions options -> createHierarchicalRequest(request, conversion, options);
            case HybridDocumentChunkOptions options -> createHybridRequest(request, conversion, options);
        };
    }

    private HierarchicalChunkDocumentRequest createHierarchicalRequest(DocumentChunkRequest request,
            ConvertDocumentRequest conversion, HierarchicalDocumentChunkOptions options) {
        var sdkOptions = chunkConverter.toSdk(options);
        // SDK 的父子 Builder 使用递归泛型，显式保留具体类型，避免链式调用丢失类型信息。
        HierarchicalChunkDocumentRequest.Builder<?, ?> builder = HierarchicalChunkDocumentRequest.builder();
        builder.sources(conversion.getSources());
        builder.options(conversion.getOptions());
        builder.target(conversion.getTarget());
        builder.includeConvertedDoc(request.includeConvertedDocument());
        builder.chunkingOptions(sdkOptions);
        return builder.build();
    }

    private HybridChunkDocumentRequest createHybridRequest(DocumentChunkRequest request,
            ConvertDocumentRequest conversion, HybridDocumentChunkOptions options) {
        var sdkOptions = chunkConverter.toSdk(options);
        // 不依赖父类方法返回值继续调用子类方法，保持编辑器与 javac 的类型解析一致。
        HybridChunkDocumentRequest.Builder<?, ?> builder = HybridChunkDocumentRequest.builder();
        builder.sources(conversion.getSources());
        builder.options(conversion.getOptions());
        builder.target(conversion.getTarget());
        builder.includeConvertedDoc(request.includeConvertedDocument());
        builder.chunkingOptions(sdkOptions);
        return builder.build();
    }

    private DocumentChunkResult convertChunkResult(ChunkDocumentResponse response) {
        if (response == null) {
            throw new DocumentParseException("解析引擎未返回分块结果", DocumentParseException.NO_HTTP_STATUS, null);
        }
        try {
            return chunkConverter.toResult(response);
        } catch (DocumentParseException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DocumentParseException("解析引擎返回无效的分块内容", DocumentParseException.NO_HTTP_STATUS, exception);
        }
    }

    private DocumentParseResult convertResult(InBodyConvertDocumentResponse result) {
        DocumentParseStatus status = converter.toParseStatus(result.getStatus());
        boolean hasContent = status == DocumentParseStatus.SUCCESS || status == DocumentParseStatus.PARTIAL_SUCCESS;
        if (hasContent && (result.getDocument() == null || result.getDocument().getJsonContent() == null)) {
            throw new DocumentParseException("解析引擎缺少请求的结构化文档", DocumentParseException.NO_HTTP_STATUS, null);
        }
        DoclingContent content = structureReader.read(result.getDocument() == null
                ? null : result.getDocument().getJsonContent());
        return converter.toResult(result, content);
    }

    private DocumentParseTask requireTask(TaskStatusPollResponse response, String expectedTaskId) {
        if (response == null || response.getTaskId() == null || response.getTaskId().isBlank()
                || response.getTaskStatus() == null
                || (expectedTaskId != null && !expectedTaskId.equals(response.getTaskId()))) {
            throw new DocumentParseException("解析引擎返回无效的任务信息", DocumentParseException.NO_HTTP_STATUS, null);
        }
        try {
            validateTaskId(response.getTaskId());
        } catch (IllegalArgumentException exception) {
            throw new DocumentParseException("解析引擎返回无效的任务标识", DocumentParseException.NO_HTTP_STATUS, exception);
        }
        return converter.toTask(response);
    }

    private void validateTaskId(String taskId) {
        Assert.hasText(taskId, "解析任务标识不能为空");
        Assert.isTrue(taskId.length() <= MAX_TASK_ID_LENGTH && TASK_ID_PATTERN.matcher(taskId).matches(), "解析任务标识格式无效");
    }

    private void validateFormat(DocumentParseRequest request) {
        String filename = request.filename();
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        DoclingFileFormat format = DoclingFileFormat.fromExtension(extension);
        DocumentParseOptions options = request.options();
        if (!format.supportsOcrAndPageRange()) {
            Assert.isTrue(!options.ocr() && !options.forceOcr() && options.ocrLanguages().isEmpty()
                    && options.firstPage() == null, "该格式不支持OCR或页码范围选项");
            Assert.isTrue(options.advanced() == null || !options.advanced().requiresPdfPipeline(),
                    "该格式不支持PDF/图片流水线的高级解析选项");
        }
    }

    private ConvertDocumentRequest createRequest(DocumentParseRequest request) {
        String encoded = readFile(request);
        var source = FileSource.builder().filename(request.filename()).base64String(encoded).build();
        ConvertDocumentOptions options = createOptions(request.options());
        var target = InBodyTarget.builder().build();
        // 转换请求同样继承递归泛型 Builder，逐项赋值以保留具体构建器类型。
        ConvertDocumentRequest.Builder<?, ?> builder = ConvertDocumentRequest.builder();
        builder.source(source);
        builder.options(options);
        builder.target(target);
        return builder.build();
    }

    private String readFile(DocumentParseRequest request) {
        // SDK 使用 Base64 JSON；读取上限以实际字节为准，不信任外部声明的文件长度。
        try (var input = request.resource().getInputStream()) {
            byte[] bytes = input.readNBytes(maxFileBytes + 1);
            Assert.isTrue(bytes.length > 0, "文档文件不能为空");
            Assert.isTrue(bytes.length <= maxFileBytes, "文档超过配置的大小限制");
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException exception) {
            throw new DocumentParseException("读取待解析文档失败", DocumentParseException.NO_HTTP_STATUS, exception);
        }
    }

    private ConvertDocumentOptions createOptions(DocumentParseOptions options) {
        var builder = ConvertDocumentOptions.builder().doOcr(options.ocr()).forceOcr(options.forceOcr())
                .ocrLang(options.ocrLanguages()).doTableStructure(options.tableStructure())
                .documentTimeout(options.documentTimeout()).toFormat(OutputFormat.MARKDOWN)
                .toFormat(OutputFormat.JSON).imageExportMode(ImageRefMode.PLACEHOLDER).includeImages(false);
        if (options.firstPage() != null) {
            var range = List.of(options.firstPage(), options.lastPage());
            builder.pageRange(range);
        }
        return builder.build();
    }

    private <T> T invoke(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DoclingServeClientException exception) {
            // 上游 SDK 包装 InterruptedException 后不恢复中断标记，在边界显式恢复。
            if (exception.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DocumentParseException("文档解析服务调用失败", exception.getStatusCode(), exception);
        } catch (ValidationException exception) {
            throw new DocumentParseException("文档解析服务拒绝请求参数", HttpStatus.UNPROCESSABLE_ENTITY.value(), exception);
        } catch (RuntimeException exception) {
            throw new DocumentParseException("文档解析服务响应或请求处理失败", DocumentParseException.NO_HTTP_STATUS, exception);
        }
    }
}
