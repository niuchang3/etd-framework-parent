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
import java.util.regex.Matcher;
import org.etd.framework.starter.ai.document.model.DocumentChunkOptions;
import org.etd.framework.starter.ai.document.model.DocumentChunkRequest;
import org.etd.framework.starter.ai.document.model.DocumentChunkResult;
import org.etd.framework.starter.ai.document.model.DocumentParseOptions;
import org.etd.framework.starter.ai.document.model.DocumentParseRequest;
import org.etd.framework.starter.ai.document.model.DocumentParseResult;
import org.etd.framework.starter.ai.document.model.DocumentParseTask;
import org.etd.framework.starter.ai.document.parser.DocumentParser;
import org.etd.framework.starter.ai.document.parser.DocumentRegexChunker;

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
        DocumentParser engine = registry.require(DoclingDocumentParser.ENGINE);
        if (!(engine instanceof AsynchronousDocumentParser parser) || !(engine instanceof DocumentChunker chunker)) {
            throw new ApiRuntimeException("文档解析及分片服务未启用，请联系管理员");
        }
        DocumentChunkParameterConverter converter = DocumentChunkParameterConverter.INSTANCE;
        DocumentParseOptions options = converter.toParseOptions(parameters);
        DocumentParseRequest document = new DocumentParseRequest(resource, filename, options);
        Instant deadline = Instant.now().plus(options.documentTimeout()).plus(KnowledgeChunkLimits.REQUEST_GRACE);
        if (parameters.basic().strategy() == DocumentChunkStrategy.REGEX) {
            return generateRegex(parser, document, parameters, deadline);
        }
        DocumentChunkOptions chunkOptions = converter.toNativeChunkOptions(parameters);
        DocumentChunkRequest request = new DocumentChunkRequest(document, chunkOptions, true);
        DocumentParseTask task = chunker.submitChunk(request);
        awaitTask(parser, task, deadline, parameters.advanced().documentTimeout());
        DocumentChunkResult result = chunker.fetchChunkResult(task.taskId());
        requireCompleteDocuments(result, parameters.advanced().documentTimeout());
        return result.chunks();
    }

    private List<DocumentChunk> generateRegex(AsynchronousDocumentParser parser, DocumentParseRequest request,
            DocumentChunkParameters parameters, Instant deadline) {
        DocumentParseTask task = parser.submit(request);
        awaitTask(parser, task, deadline, parameters.advanced().documentTimeout());
        DocumentParseResult result = parser.fetchResult(task.taskId());
        requireCompleteDocument(result, parameters.advanced().documentTimeout());
        DocumentRegexChunker chunker = new DocumentRegexChunker();
        return chunker.chunk(request.filename(), result, parameters);
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
            Matcher progress = TIMEOUT_PROGRESS.matcher(warning);
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

    /** 先检查统一截止时间，再等待轮询间隔并查询状态；外部IO不占用数据库事务。 */
    private void awaitTask(AsynchronousDocumentParser parser, DocumentParseTask initialTask, Instant deadline, Duration timeout) {
        DocumentParseTask currentTask = initialTask;
        // 只有完成或失败才结束轮询；截止时间沿用提交前计算的预算，不在每次查询后重新计时。
        while (currentTask.status() != DocumentTaskStatus.COMPLETED && currentTask.status() != DocumentTaskStatus.FAILED) {
            if (Instant.now().isAfter(deadline)) {
                Duration budget = timeout.plus(KnowledgeChunkLimits.REQUEST_GRACE);
                throw KnowledgeChunkGenerationException.waitTimeout(budget);
            }
            try {
                Thread.sleep(KnowledgeChunkLimits.POLL_MILLIS);
            } catch (InterruptedException exception) {
                // 保留中断标志，阻止被取消的请求继续发起下一次远端查询。
                Thread.currentThread().interrupt();
                throw new KnowledgeChunkGenerationException(Reason.INTERRUPTED);
            }
            currentTask = parser.fetchStatus(initialTask.taskId());
        }
        if (currentTask.status() == DocumentTaskStatus.FAILED) {
            throw new KnowledgeChunkGenerationException(Reason.TASK_FAILED);
        }
    }
}
