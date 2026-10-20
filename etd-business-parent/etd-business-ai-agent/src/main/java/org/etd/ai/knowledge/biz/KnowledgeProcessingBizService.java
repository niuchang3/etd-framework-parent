package org.etd.ai.knowledge.biz;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.entity.*;
import org.etd.ai.knowledge.converter.KnowledgeChunkConverter;
import org.etd.ai.knowledge.service.*;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;

/** 文档处理编排；外部IO在事务外执行，短事务内核对租约和轮次后发布结果。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeProcessingBizService {
    /** 单次处理租约10分钟，向量化每完成一个分片后续租。 */
    private static final long LEASE_SECONDS = 600;
    private final KnowledgeDocumentService documents;
    private final KnowledgeDocumentBizService documentBiz;
    private final KnowledgeObjectService objects;
    private final KnowledgeTextService texts;
    private final KnowledgeChunkService chunks;
    private final KnowledgeVectorService vectors;
    private final KnowledgeEmbeddingService embeddings;
    private final ModelConnectionService connections;
    private final ModelService models;
    private final TransactionTemplate transactions;

    /** @param id 资料ID @return 当前解析轮次详情；再次调用重新解析并使旧轮次失效 */
    public KnowledgeDocumentEntity createPreview(Long id) {
        var task = transactions.execute(status -> beginParsing(id));
        try {
            String text;
            try (var input = objects.openObject(task)) {
                text = texts.parseText(input, task.getOriginalFilename());
            }
            var contents = texts.splitText(text, task.getChunkMaxTokens(), task.getChunkOverlapTokens());
            var records = createChunkList(task, contents);
            return transactions.execute(status -> finishParsing(task, records));
        } catch (Exception exception) {
            failProcessing(task, exception);
            if (exception instanceof ApiRuntimeException business) {
                throw business;
            }
            throw new ApiRuntimeException("正文解析失败，请检查文件内容和处理配置");
        }
    }

    /** 新轮次开始即隐藏旧结果；物理旧结果在新正文提交时清理。 */
    private KnowledgeDocumentEntity beginParsing(Long id) {
        var document = documents.requireDocument(id, true);
        documentBiz.requireIdleProcessing(document);
        requireUploaded(document);
        document.setProcessingVersion(document.getProcessingVersion() + 1);
        populateLease(document, KnowledgePolicy.PARSING);
        document.setParserVersion("jdk-utf8-docx-v1");
        document.setSplitterVersion("cl100k-base-unicode-v1");
        documents.modifyDocument(document);
        return document;
    }

    /** 将解析结果转换为本轮实体；正文及权限副本全部来自可信资料。 */
    private List<KnowledgeChunkEntity> createChunkList(KnowledgeDocumentEntity document, List<String> contents) {
        if (contents.isEmpty() || contents.size() > KnowledgePolicy.MAX_CHUNKS) {
            throw new ApiRuntimeException("没有有效正文或分片数量超过10000");
        }
        var result = new ArrayList<KnowledgeChunkEntity>();
        for (int index = 0; index < contents.size(); index++) {
            var chunk = KnowledgeChunkConverter.INSTANCE.toEntity(document);
            chunk.setId(IdWorker.getId());
            chunk.setChunkIndex(index);
            chunk.setContent(contents.get(index));
            chunk.setTokenCount(texts.countTokens(contents.get(index)));
            chunk.setVectorId(IdWorker.getId());
            chunk.setEmbeddingStatus(KnowledgePolicy.PENDING);
            result.add(chunk);
        }
        return result;
    }

    /** 旧任务即使晚完成也不能覆盖新轮次。 */
    private KnowledgeDocumentEntity finishParsing(KnowledgeDocumentEntity task, List<KnowledgeChunkEntity> records) {
        var document = requireTask(task);
        chunks.removeChunkList(document.getId());
        chunks.createChunkList(records);
        releaseLease(document, KnowledgePolicy.PREVIEW);
        documents.modifyDocument(document);
        return document;
    }

    /** @param id 已预览资料ID @return 确认并完成索引后的资料；全文模式不调用模型 */
    public KnowledgeDocumentEntity approvePreview(Long id) {
        var task = transactions.execute(status -> beginEmbedding(id));
        if (KnowledgePolicy.READY.equals(task.getProcessingStatus())) {
            return task;
        }
        try {
            var records = chunks.fetchChunkList(task.getId(), task.getProcessingVersion());
            if (!KnowledgePolicy.KEYWORD.equals(task.getRetrievalMode())) {
                createVectorList(task, records);
            }
            return transactions.execute(status -> finishEmbedding(task, records));
        } catch (Exception exception) {
            failProcessing(task, exception);
            if (exception instanceof ApiRuntimeException business) {
                throw business;
            }
            throw new ApiRuntimeException("资料索引处理失败，请重试");
        }
    }

    /** 重复确认已完成资料幂等；失败时允许复用现有正文重试向量化。 */
    private KnowledgeDocumentEntity beginEmbedding(Long id) {
        var document = documents.requireDocument(id, true);
        documentBiz.requireIdleProcessing(document);
        requireUploaded(document);
        if (KnowledgePolicy.READY.equals(document.getProcessingStatus())) {
            return document;
        }
        var records = chunks.fetchChunkList(id, document.getProcessingVersion());
        if (records.isEmpty() || !(KnowledgePolicy.PREVIEW.equals(document.getProcessingStatus())
                || KnowledgePolicy.FAILED.equals(document.getProcessingStatus())
                || KnowledgePolicy.EMBEDDING.equals(document.getProcessingStatus()))) {
            throw new ApiRuntimeException("请先解析并预览正文分片");
        }
        populateLease(document, KnowledgePolicy.EMBEDDING);
        chunks.removeVectorList(id);
        documents.modifyDocument(document);
        return document;
    }

    /** 模型调用不占用数据库事务，每个分片写入时核对并续租。 */
    private void createVectorList(KnowledgeDocumentEntity task, List<KnowledgeChunkEntity> records) {
        var connection = connections.requireConnection(task.getEmbeddingModelConnectionId(), false);
        var model = models.requireModel(connection.getModelId());
        documentBiz.validateConfiguration(task);
        for (KnowledgeChunkEntity chunk : records) {
            var values = embeddings.createEmbedding(connection, model, task.getEmbedding(), chunk.getContent());
            transactions.executeWithoutResult(status -> {
                var document = requireTask(task);
                vectors.createVector(document, chunk, values);
                document.setProcessingLeaseUntil(Instant.now().plusSeconds(LEASE_SECONDS));
                documents.modifyDocument(document);
            });
        }
    }

    /** 当前轮次全部成功后一次发布，部分向量不参与可检索状态。 */
    private KnowledgeDocumentEntity finishEmbedding(KnowledgeDocumentEntity task, List<KnowledgeChunkEntity> records) {
        var document = requireTask(task);
        boolean keyword = KnowledgePolicy.KEYWORD.equals(document.getRetrievalMode());
        for (KnowledgeChunkEntity chunk : records) {
            chunk.setEmbeddingStatus(keyword ? KnowledgePolicy.NOT_REQUIRED : KnowledgePolicy.READY);
            if (!keyword) {
                chunk.setEmbeddedTime(Instant.now());
            }
        }
        chunks.modifyChunkList(records);
        releaseLease(document, KnowledgePolicy.READY);
        document.setProcessedTime(Instant.now());
        documents.modifyDocument(document);
        return document;
    }

    /** 仅标记仍属于本任务的失败，禁止旧任务污染新轮次；异常内容不直接持久化。 */
    private void failProcessing(KnowledgeDocumentEntity task, Exception exception) {
        log.warn("知识资料处理失败，资料ID={}，轮次={}，异常类型={}", task.getId(), task.getProcessingVersion(),
                exception.getClass().getSimpleName());
        transactions.executeWithoutResult(status -> {
            var document = documents.requireDocument(task.getId(), true);
            if (Objects.equals(document.getProcessingOwner(), task.getProcessingOwner())
                    && Objects.equals(document.getProcessingVersion(), task.getProcessingVersion())) {
                releaseLease(document, KnowledgePolicy.FAILED);
                document.setErrorMessage("资料处理失败，请检查文件内容、模型连接器和处理配置后重试");
                document.setProcessedTime(Instant.now());
                documents.modifyDocument(document);
            }
        });
    }

    /** 核对任务身份、轮次及租约有效性，不能只检查状态字符串。 */
    private KnowledgeDocumentEntity requireTask(KnowledgeDocumentEntity task) {
        var document = documents.requireDocument(task.getId(), true);
        if (!Objects.equals(document.getProcessingOwner(), task.getProcessingOwner())
                || !Objects.equals(document.getProcessingVersion(), task.getProcessingVersion())
                || document.getProcessingLeaseUntil() == null
                || !document.getProcessingLeaseUntil().isAfter(Instant.now())) {
            throw new ApiRuntimeException("处理任务已过期或被新的处理轮次替代");
        }
        return document;
    }

    /** 初始化当前阶段的任务租约，清除上一阶段失败信息。 */
    private void populateLease(KnowledgeDocumentEntity document, String state) {
        document.setProcessingStatus(state);
        document.setProcessingOwner(UUID.randomUUID());
        document.setProcessingStartedTime(Instant.now());
        document.setProcessingLeaseUntil(Instant.now().plusSeconds(LEASE_SECONDS));
        document.setErrorMessage(null);
        document.setProcessedTime(null);
    }

    /** 离开处理阶段时显式清空租约，符合数据库状态约束。 */
    private void releaseLease(KnowledgeDocumentEntity document, String state) {
        document.setProcessingStatus(state);
        document.setProcessingOwner(null);
        document.setProcessingLeaseUntil(null);
    }

    /** 原件未经完整性核验不得进入解析或模型调用阶段。 */
    private void requireUploaded(KnowledgeDocumentEntity document) {
        if (!KnowledgePolicy.COMPLETED.equals(document.getUploadStatus())) {
            throw new ApiRuntimeException("原件尚未上传完成");
        }
    }
}
