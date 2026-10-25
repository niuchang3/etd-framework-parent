package org.etd.ai.knowledge.service;

import java.time.Instant;
import java.time.Duration;
import java.util.regex.Pattern;
import org.etd.ai.knowledge.exception.KnowledgeChunkGenerationException;
import org.etd.ai.knowledge.exception.KnowledgeChunkGenerationException.Reason;
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
    /** Docling标准超时诊断，只用于分类，不直接把远端文本回显到接口。 */
    private static final String DOCUMENT_TIMEOUT_WARNING = "document timeout exceeded";
    /** 只提取标准流水线诊断中的有限长度数字，正文及其他远端信息均不能进入提示。 */
    private static final Pattern TIMEOUT_PROGRESS = Pattern.compile(
            "Pipeline stage timeout: processed ([0-9]{1,9})/([0-9]{1,9}) pages successfully, "
                    + "([0-9]{1,9}) pages failed or incomplete\\.");
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
        awaitTask(parser, task, deadline, parameters.advanced().documentTimeout());
        var result = chunker.fetchChunkResult(task.taskId());
        requireCompleteDocuments(result, parameters.advanced().documentTimeout());
        var overlap = new DocumentChunkOverlapProcessor();
        var blocks = result.documents().getFirst().result().blocks();
        return overlap.apply(result.chunks(), blocks, parameters);
    }

    private List<DocumentChunk> generateRegex(AsynchronousDocumentParser parser, DocumentParseRequest request,
            DocumentChunkParameters parameters, Instant deadline) {
        var task = parser.submit(request);
        awaitTask(parser, task, deadline, parameters.advanced().documentTimeout());
        var result = parser.fetchResult(task.taskId());
        requireCompleteDocument(result, parameters.advanced().documentTimeout());
        var chunker = new DocumentRegexChunker();
        var chunks = chunker.chunk(request.filename(), result, parameters);
        var overlap = new DocumentChunkOverlapProcessor();
        return overlap.apply(chunks, result.blocks(), parameters);
    }

    /** 单文件生成必须返回唯一结果，不能把结果数量异常伪装成解析超时。 */
    private void requireCompleteDocuments(DocumentChunkResult result, Duration timeout) {
        if (result.documents().size() != 1) {
            throw new KnowledgeChunkGenerationException(Reason.INVALID_RESULT_COUNT);
        }
        requireCompleteDocument(result.documents().getFirst().result(), timeout);
    }

    /** 正则和原生分片统一识别文档级失败；任务完成不代表文档完整解析成功。 */
    private void requireCompleteDocument(DocumentParseResult result, Duration timeout) {
        if (result.status() == DocumentParseStatus.SUCCESS) {
            return;
        }
        requireNoParseTimeout(result, timeout);
        Reason reason = switch (result.status()) {
            case PARTIAL_SUCCESS -> Reason.PARTIAL_PARSE;
            case SKIPPED -> Reason.DOCUMENT_SKIPPED;
            default -> Reason.PARSE_FAILED;
        };
        throw new KnowledgeChunkGenerationException(reason);
    }

    /** 只识别明确超时证据，不能根据partial_success或耗时接近时限就推测超时。 */
    private void requireNoParseTimeout(DocumentParseResult result, Duration timeout) {
        boolean timedOut = false;
        for (String warning : result.warnings()) {
            if (DOCUMENT_TIMEOUT_WARNING.equals(warning)) {
                timedOut = true;
            }
            var progress = TIMEOUT_PROGRESS.matcher(warning);
            if (progress.matches()) {
                int completed = Integer.parseInt(progress.group(1));
                int total = Integer.parseInt(progress.group(2));
                int failed = Integer.parseInt(progress.group(3));
                boolean valid = completed <= total && completed + failed == total;
                Integer completedPages = valid ? completed : null;
                Integer totalPages = valid ? total : null;
                throw KnowledgeChunkGenerationException.parseTimeout(timeout, completedPages, totalPages);
            }
        }
        if (timedOut) {
            throw KnowledgeChunkGenerationException.parseTimeout(timeout, null, null);
        }
    }

    /** 外部IO不占用数据库事务；线程被中断时及时退出并恢复中断标志。 */
    private void awaitTask(AsynchronousDocumentParser parser, DocumentParseTask initial, Instant deadline, Duration timeout) {
        var task = initial;
        while (task.status() != DocumentTaskStatus.COMPLETED && task.status() != DocumentTaskStatus.FAILED) {
            if (Instant.now().isAfter(deadline)) {
                Duration budget = timeout.plus(KnowledgeChunkLimits.REQUEST_GRACE);
                throw KnowledgeChunkGenerationException.waitTimeout(budget);
            }
            try { Thread.sleep(KnowledgeChunkLimits.POLL_MILLIS); }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new KnowledgeChunkGenerationException(Reason.INTERRUPTED);
            }
            task = parser.fetchStatus(initial.taskId());
        }
        if (task.status() == DocumentTaskStatus.FAILED) {
            throw new KnowledgeChunkGenerationException(Reason.TASK_FAILED);
        }
    }
}
