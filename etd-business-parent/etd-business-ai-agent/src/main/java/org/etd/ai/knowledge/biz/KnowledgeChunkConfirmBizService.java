package org.etd.ai.knowledge.biz;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgeChunkStatus;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.entity.KnowledgeChunkEntity;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeChunkService;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeModelSettingsService;
import org.etd.ai.knowledge.service.KnowledgeVectorService;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.service.ModelInferenceService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 人工确认用例：事务外推理，提交前重新校验资料和模型配置版本，原子发布确认状态及向量。 */
@Service
@RequiredArgsConstructor
public class KnowledgeChunkConfirmBizService {
    private final KnowledgeDocumentService documents;
    private final KnowledgeBaseService bases;
    private final KnowledgeChunkService chunks;
    private final KnowledgeModelSettingsService settings;
    private final ModelInferenceService inference;
    private final KnowledgeVectorService vectors;
    private final TransactionTemplate transactions;

    /**
     * @param id 资料ID
     * @param version 用户当前看到的资料版本
     * @return 完成当前检索模式索引的已确认资料；不自动启用正式检索
     * @throws ApiRuntimeException 权限、内容、配置已变化或模型调用失败，原有状态保持不变
     */
    public KnowledgeDocumentEntity confirmChunks(Long id, Long version) {
        var snapshot = transactions.execute(status -> readSnapshot(id, version));
        List<float[]> result = List.of();
        if (!KnowledgeRetrievalPolicy.KEYWORD.equals(snapshot.base().getRetrievalMode())) {
            var base = snapshot.base();
            settings.validate(base.getEmbeddingConnectionId(), ModelType.EMBEDDING, base.getDimensions());
            var texts = snapshot.chunks().stream().map(KnowledgeChunkEntity::getContent).toList();
            result = inference.createEmbeddingList(base.getEmbeddingConnectionId(), base.getDimensions(), texts);
        }
        var embeddings = result;
        return transactions.execute(status -> publishConfirmation(snapshot, embeddings));
    }

    /** 读取一致快照后即释放行锁，外部模型请求期间不占用数据库事务。 */
    private ConfirmationSnapshot readSnapshot(Long id, Long version) {
        var document = requireConfirmable(id, version);
        var base = bases.requireBase(document.getKnowledgeBaseId(), true, true);
        var records = chunks.fetchChunkList(id);
        if (records.isEmpty() || records.size() != document.getChunkCount()) {
            throw new ApiRuntimeException("分片数量已变化，请刷新后重试");
        }
        return new ConfirmationSnapshot(document, base, records);
    }

    /** 两次读取使用相同锁顺序；编辑或知识库设置变化会使在途结果失去发布资格。 */
    private KnowledgeDocumentEntity publishConfirmation(ConfirmationSnapshot snapshot, List<float[]> embeddings) {
        var document = requireConfirmable(snapshot.document().getId(), snapshot.document().getVersion());
        var base = bases.requireBase(document.getKnowledgeBaseId(), true, true);
        if (!Objects.equals(base.getVersion(), snapshot.base().getVersion())) {
            throw new ApiRuntimeException("知识库配置已变化，请重新确认分片");
        }
        if (!embeddings.isEmpty()) {
            activateIndex(base);
        }
        vectors.replaceVectorList(document, base, snapshot.chunks(), embeddings);
        document.setChunkStatus(KnowledgeChunkStatus.CONFIRMED);
        document.setChunkConfirmedTime(Instant.now());
        documents.modifyDocument(document);
        return document;
    }

    /** 模型或维度切换使用新代次；其他资料须重新确认后才能进入这一向量空间。 */
    private void activateIndex(KnowledgeBaseEntity base) {
        if (Objects.equals(base.getActiveConnectionId(), base.getEmbeddingConnectionId())
                && Objects.equals(base.getActiveDimensions(), base.getDimensions())) {
            return;
        }
        base.setActiveIndexVersion(Math.incrementExact(base.getActiveIndexVersion()));
        base.setActiveConnectionId(base.getEmbeddingConnectionId());
        base.setActiveDimensions(base.getDimensions());
        bases.modifyBase(base);
    }

    /** 人工确认和发布均要求维护权限、上传完成、有效分片及同一资料版本。 */
    private KnowledgeDocumentEntity requireConfirmable(Long id, Long version) {
        var document = documents.requireDocument(id, true);
        documents.requireChunkIdle(document);
        if (!Objects.equals(document.getVersion(), version)) {
            throw new ApiRuntimeException("分片已变化，请刷新后重试");
        }
        if (!KnowledgePolicy.COMPLETED.equals(document.getUploadStatus())
                || document.getChunkCount() == null || document.getChunkCount() == 0) {
            throw new ApiRuntimeException("请先完成上传并生成有效分片");
        }
        return document;
    }

    /** @param document 当前资料及版本 @param base 当前知识库配置及版本 @param chunks 人工确认的权威正文 */
    private record ConfirmationSnapshot(KnowledgeDocumentEntity document, KnowledgeBaseEntity base,
            List<KnowledgeChunkEntity> chunks) { }
}
