package org.etd.ai.knowledge.biz;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.time.Instant;
import java.net.SocketException;
import java.net.http.HttpTimeoutException;
import org.etd.framework.starter.ai.document.exception.DocumentParseException;
import org.etd.ai.knowledge.exception.KnowledgeChunkGenerationException.Reason;
import org.etd.framework.common.core.constants.BasicConstant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.etd.ai.knowledge.constant.*;
import org.etd.ai.knowledge.converter.KnowledgeChunkConverter;
import org.etd.ai.knowledge.entity.*;
import org.etd.ai.knowledge.service.*;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.ai.knowledge.exception.KnowledgeChunkGenerationException;
import org.etd.framework.starter.ai.document.docling.DoclingProperties;
import org.etd.framework.starter.ai.document.model.DocumentChunk;
import org.etd.framework.starter.ai.document.exception.DocumentChunkException;
import org.etd.framework.starter.ai.document.model.DocumentChunkParameters;
import org.etd.framework.starter.ai.document.tokenizer.DocumentTokenCounter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 手动生成分片：先记录状态，外部解析成功后在短事务中原子替换旧结果。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeChunkGenerationBizService {
    private final KnowledgeDocumentService documents;
    private final KnowledgeObjectService objects;
    private final KnowledgeChunkService chunks;
    private final KnowledgeChunkGenerationService generation;
    private final DoclingProperties properties;
    private final TransactionTemplate transactions;

    /** @param id 资料ID @param version 当前资料版本 @param parameters 三级参数 @return 最新状态，失败时保留旧结果并返回failed */
    public KnowledgeDocumentEntity generateChunks(Long id, Long version, DocumentChunkParameters parameters) {
        var task = transactions.execute(status -> beginGeneration(id, version, parameters));
        try {
            var resource = readResource(task);
            var result = generation.generateChunkList(resource, task.getOriginalFilename(), parameters);
            var records = createChunkList(task, result);
            return transactions.execute(status -> finishGeneration(task, parameters, records));
        } catch (Exception exception) {
            String failureMessage = resolveFailureMessage(exception);
            String reason = resolveFailureReason(exception);
            // Throwable作为最后一个日志参数记录完整堆栈及cause链；不额外输出正文、规则或认证参数。
            log.error("知识资料分片生成失败，资料ID={}，生成请求ID={}，分片策略={}，异常类型={}，原因={}，失败说明={}",
                    id, task.getChunkGenerationId(), parameters.basic().strategy(),
                    exception.getClass().getSimpleName(), reason, failureMessage, exception);
            return transactions.execute(status -> failGeneration(task, failureMessage));
        }
    }

    private KnowledgeDocumentEntity beginGeneration(Long id, Long version, DocumentChunkParameters parameters) {
        var document = documents.requireDocument(id, true);
        documents.requireChunkIdle(document);
        if (!Objects.equals(document.getVersion(), version)) { throw new ApiRuntimeException("资料已变化，请刷新后重试"); }
        if (!KnowledgePolicy.COMPLETED.equals(document.getUploadStatus())) { throw new ApiRuntimeException("请先完成原件上传"); }
        // 重新生成会使正在执行的确认请求失去发布资格，避免留下永久processing状态。
        if (KnowledgeVectorStatus.PROCESSING.equals(document.getVectorStatus())) {
            document.setVectorStatus(KnowledgeVectorStatus.STALE);
        }
        document.setChunkGenerationId(IdWorker.getId());
        document.setChunkGenerationExpiresAt(Instant.now().plus(parameters.advanced().documentTimeout())
                .plus(KnowledgeChunkLimits.REQUEST_GRACE));
        document.setChunkStatus(KnowledgeChunkStatus.GENERATING);
        document.setChunkErrorMessage(null);
        documents.modifyDocument(document);
        return document;
    }

    private ByteArrayResource readResource(KnowledgeDocumentEntity document) throws Exception {
        if (document.getFileSize() > properties.getMaxFileBytes()) { throw new ApiRuntimeException("原件超过单次文档解析的大小限制"); }
        byte[] bytes;
        try (var input = objects.openObject(document)) { bytes = input.readNBytes(properties.getMaxFileBytes() + 1); }
        if (bytes.length == 0 || bytes.length > properties.getMaxFileBytes()) { throw new ApiRuntimeException("原件为空或超过解析大小限制"); }
        return new ByteArrayResource(bytes);
    }

    private List<KnowledgeChunkEntity> createChunkList(KnowledgeDocumentEntity document, List<DocumentChunk> result) {
        if (result.isEmpty() || result.size() > KnowledgeChunkLimits.MAX_CHUNKS) { throw new ApiRuntimeException("无有效正文或分片数量超过限制"); }
        List<KnowledgeChunkEntity> records = new ArrayList<>();
        for (DocumentChunk chunk : result) {
            String content = chunk.rawText();
            if (content == null || content.isBlank() || content.length() > KnowledgeChunkLimits.MAX_CONTENT_CHARACTERS) {
                throw new ApiRuntimeException("分片正文为空或超过长度限制，请调整参数");
            }
            var record = KnowledgeChunkConverter.INSTANCE.toEntity(document, chunk);
            record.setTokenCount(DocumentTokenCounter.count(content));
            records.add(record);
        }
        return records;
    }

    private KnowledgeDocumentEntity finishGeneration(KnowledgeDocumentEntity task, DocumentChunkParameters parameters,
            List<KnowledgeChunkEntity> records) {
        var document = requireCurrentGeneration(task);
        document.setDataStatus(BasicConstant.DataStatus.DISABLED_CODE);
        document.setProcessingVersion(Math.incrementExact(document.getProcessingVersion()));
        document.setVectorStatus(KnowledgeVectorStatus.afterContentChange(document.getVectorStatus()));
        chunks.removeChunkList(document.getId());
        for (var record : records) {
            record.setProcessingVersion(document.getProcessingVersion());
        }
        chunks.createChunkList(records);
        document.setChunkParameters(parameters);
        document.setChunkCount(records.size());
        document.setChunkStatus(KnowledgeChunkStatus.DRAFT);
        document.setChunkConfirmedTime(null);
        releaseGeneration(document);
        documents.modifyDocument(document);
        return document;
    }

    /** 只允许固定规则及受控数字诊断进入接口，其他异常仍脱敏，避免泄露SDK响应或文档正文。 */
    private String resolveFailureMessage(Exception exception) {
        if (exception instanceof KnowledgeChunkGenerationException generationException) {
            return generationException.getMessage() + "；已有分片已保留";
        }
        if (exception instanceof DocumentChunkException chunkException) {
            return chunkException.getReason().getMessage() + "；已有分片已保留";
        }
        if (exception instanceof DocumentParseException parseException) {
            return resolveServiceFailureReason(parseException).getMessage() + "；已有分片已保留";
        }
        return "分片生成失败，请检查文档格式、解析参数或联系管理员；已有分片已保留";
    }

    /** 日志使用明确分类，不从任意第三方异常消息中推断或暴露正文。 */
    private String resolveFailureReason(Exception exception) {
        if (exception instanceof KnowledgeChunkGenerationException generationException) {
            return generationException.getReason().getCode();
        }
        if (exception instanceof DocumentChunkException chunkException) {
            return chunkException.getReason().getCode();
        }
        if (exception instanceof DocumentParseException parseException) {
            return resolveServiceFailureReason(parseException).getCode();
        }
        return exception.getClass().getSimpleName();
    }

    /** 仅按异常类型区分连接中断与HTTP超时，不解析或回显可能含正文、地址和凭证的远端消息。 */
    private Reason resolveServiceFailureReason(DocumentParseException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof HttpTimeoutException) {
                return Reason.SERVICE_REQUEST_TIMEOUT;
            }
            if (cause instanceof SocketException) {
                return Reason.SERVICE_CONNECTION_INTERRUPTED;
            }
            cause = cause.getCause();
        }
        return Reason.SERVICE_CALL_FAILED;
    }

    /** 只更新仍属于当前请求的失败；超时后重试的新结果不会被旧请求覆盖。 */
    private KnowledgeDocumentEntity failGeneration(KnowledgeDocumentEntity task, String failureMessage) {
        var document = documents.requireDocument(task.getId(), true);
        if (!Objects.equals(document.getChunkGenerationId(), task.getChunkGenerationId())) {
            throw new ApiRuntimeException("本次生成请求已失效，当前分片未被替换，请刷新查看");
        }
        document.setChunkStatus(KnowledgeChunkStatus.FAILED);
        document.setChunkErrorMessage(failureMessage);
        releaseGeneration(document);
        documents.modifyDocument(document);
        return document;
    }

    private KnowledgeDocumentEntity requireCurrentGeneration(KnowledgeDocumentEntity task) {
        var document = documents.requireDocument(task.getId(), true);
        if (!Objects.equals(document.getChunkGenerationId(), task.getChunkGenerationId())
                || document.getChunkGenerationExpiresAt() == null || Instant.now().isAfter(document.getChunkGenerationExpiresAt())) {
            throw new ApiRuntimeException("生成请求已过期，不能覆盖当前分片");
        }
        return document;
    }

    private void releaseGeneration(KnowledgeDocumentEntity document) {
        document.setChunkGenerationId(null);
        document.setChunkGenerationExpiresAt(null);
    }
}
