package org.etd.ai.knowledge.service;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgeChunkLimits;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.constant.DocumentParseStatus;
import org.etd.framework.starter.ai.document.constant.DocumentTaskStatus;
import org.etd.framework.starter.ai.document.converter.DocumentChunkParameterConverter;
import org.etd.framework.starter.ai.document.docling.DoclingDocumentParser;
import org.etd.framework.starter.ai.document.model.*;
import org.etd.framework.starter.ai.document.parser.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/** 单次手动请求中的Docling解析和分片能力，不创建后台任务编排或向量链路。 */
@Service
@RequiredArgsConstructor
public class KnowledgeChunkGenerationService {
    private final DocumentParserRegistry registry;
    /** @param resource 已授权原件 @param filename 原始文件名 @param parameters 三级参数 @return 完整成功的分片草稿 */
    public List<DocumentChunk> generateChunkList(Resource resource, String filename, DocumentChunkParameters parameters) {
        var engine = registry.require(DoclingDocumentParser.ENGINE);
        if (!(engine instanceof AsynchronousDocumentParser parser) || !(engine instanceof DocumentChunker chunker)) {
            throw new ApiRuntimeException("未启用Docling解析及分片服务");
        }
        var converter = DocumentChunkParameterConverter.INSTANCE;
        var options = converter.toParseOptions(parameters);
        var document = new DocumentParseRequest(resource, filename, options);
        Instant deadline = Instant.now().plus(options.documentTimeout()).plus(KnowledgeChunkLimits.REQUEST_GRACE);
        if (parameters.basic().strategy() == DocumentChunkStrategy.REGEX) {
            return generateRegex(parser, document, parameters, deadline);
        }
        var chunkOptions = converter.toNativeChunkOptions(parameters);
        var request = new DocumentChunkRequest(document, chunkOptions, true);
        var task = chunker.submitChunk(request);
        awaitTask(parser, task, deadline);
        var result = chunker.fetchChunkResult(task.taskId());
        requireCompleteDocuments(result);
        var overlap = new DocumentChunkOverlapProcessor();
        var blocks = result.documents().getFirst().result().blocks();
        return overlap.apply(result.chunks(), blocks, parameters);
    }

    private List<DocumentChunk> generateRegex(AsynchronousDocumentParser parser, DocumentParseRequest request,
            DocumentChunkParameters parameters, Instant deadline) {
        var task = parser.submit(request);
        awaitTask(parser, task, deadline);
        var result = parser.fetchResult(task.taskId());
        if (result.status() != DocumentParseStatus.SUCCESS) { throw new ApiRuntimeException("文档未完整解析成功，请调整参数后重试"); }
        var chunker = new DocumentRegexChunker();
        var chunks = chunker.chunk(request.filename(), result, parameters);
        var overlap = new DocumentChunkOverlapProcessor();
        return overlap.apply(chunks, result.blocks(), parameters);
    }

    private void requireCompleteDocuments(DocumentChunkResult result) {
        if (result.documents().size() != 1 || result.documents().getFirst().result().status() != DocumentParseStatus.SUCCESS) {
            throw new ApiRuntimeException("文档未完整解析成功，请调整参数后重试");
        }
    }

    /** 外部IO不占用数据库事务；线程被中断时及时退出并恢复中断标志。 */
    private void awaitTask(AsynchronousDocumentParser parser, DocumentParseTask initial, Instant deadline) {
        var task = initial;
        while (task.status() != DocumentTaskStatus.COMPLETED && task.status() != DocumentTaskStatus.FAILED) {
            if (Instant.now().isAfter(deadline)) { throw new ApiRuntimeException("Docling处理超时，请稍后重试"); }
            try { Thread.sleep(KnowledgeChunkLimits.POLL_MILLIS); }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ApiRuntimeException("分片生成请求已中断");
            }
            task = parser.fetchStatus(initial.taskId());
        }
        if (task.status() == DocumentTaskStatus.FAILED) { throw new ApiRuntimeException("Docling处理失败，请检查文档格式和解析参数"); }
    }
}
