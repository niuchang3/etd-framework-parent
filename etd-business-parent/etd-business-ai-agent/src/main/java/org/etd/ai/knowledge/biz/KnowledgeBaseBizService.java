package org.etd.ai.knowledge.biz;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.component.KnowledgeModelClient;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.converter.KnowledgeBaseConverter;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.model.constant.ModelType;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Objects;
/** 知识库配置与索引提交用例，外部模型调用由后台任务执行。 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseBizService {
    private final KnowledgeBaseService bases;
    private final KnowledgeFolderService folders;
    private final KnowledgeModelClient models;
    /** @param input 用户设置 @return 新知识库ID */
    @Transactional
    public Long createBase(KnowledgeBaseEntity input) {
        validateSettings(input);
        return bases.createBase(input);
    }
    /** @param id 知识库 @param input 可编辑设置 @param version 当前版本 */
    @Transactional
    public void modifyBase(Long id, KnowledgeBaseEntity input, Long version) {
        var base = bases.requireBase(id, true, true);
        requireVersion(base, version);
        if (KnowledgeRetrievalPolicy.BUILDING.equals(base.getIndexStatus())) {
            throw new ApiRuntimeException("索引构建中，请完成后再修改设置");
        }
        if (!Objects.equals(base.getOrgId(), input.getOrgId())) {
            throw new ApiRuntimeException("知识库创建后不允许变更组织归属");
        }
        validateSettings(input);
        // 模型或维度改变后不能复用旧失败任务；其余设置不改变向量空间。
        boolean changed = !Objects.equals(base.getEmbeddingConnectionId(), input.getEmbeddingConnectionId())
                || !Objects.equals(base.getDimensions(), input.getDimensions());
        if (changed) {
            base.setBuildingIndexVersion(0L);
            base.setIndexStatus(KnowledgeRetrievalPolicy.IDLE);
            base.setIndexCompleted(0);
            base.setIndexTotal(0);
            base.setIndexError(null);
        }
        KnowledgeBaseConverter.INSTANCE.updateEntity(input, base);
        bases.modifyBase(base);
    }
    /** @param id 知识库 @param version 当前版本 @return 本次构建代次 */
    @Transactional
    public Long initIndex(Long id, Long version) {
        var base = bases.requireBase(id, true, true);
        requireVersion(base, version);
        if (KnowledgeRetrievalPolicy.BUILDING.equals(base.getIndexStatus())) {
            throw new ApiRuntimeException("已有索引任务，请等待完成");
        }
        requireEmbedding(base);
        // 同配置失败重试继续同一代次，后台复用已完成分片，避免重复模型调用。
        if (!KnowledgeRetrievalPolicy.FAILED.equals(base.getIndexStatus()) || base.getBuildingIndexVersion() == 0) {
            base.setBuildingIndexVersion(IdWorker.getId());
        }
        base.setIndexStatus(KnowledgeRetrievalPolicy.BUILDING);
        base.setIndexTotal(0);
        base.setIndexCompleted(0);
        base.setIndexError(null);
        base.setIndexLeaseUntil(null);
        base.setIndexStartedTime(Instant.now());
        bases.modifyBase(base);
        return base.getBuildingIndexVersion();
    }
    /** @param id 空知识库，构建期间禁止删除 */
    @Transactional
    public void removeBase(Long id) {
        folders.lockStructure();
        var base = bases.requireBase(id, true, true);
        if (KnowledgeRetrievalPolicy.BUILDING.equals(base.getIndexStatus())) {
            throw new ApiRuntimeException("索引构建中不能删除知识库");
        }
        bases.removeBase(id);
        folders.removeFolderList(id);
    }
    private void requireVersion(KnowledgeBaseEntity base, Long version) {
        if (!Objects.equals(base.getVersion(), version)) {
            throw new ApiRuntimeException("知识库已变化，请刷新后重试");
        }
    }
    private void requireEmbedding(KnowledgeBaseEntity base) {
        KnowledgeRetrievalPolicy.vectorTable(base.getDimensions());
        models.validate(base.getEmbeddingConnectionId(), ModelType.EMBEDDING, base.getDimensions());
    }
    private void validateSettings(KnowledgeBaseEntity base) {
        if (base.getRerankThreshold() != null && !Double.isFinite(base.getRerankThreshold())) {
            throw new ApiRuntimeException("重排阈值必须为有限数值");
        }
        base.setName(base.getName().trim());
        if (!KnowledgeRetrievalPolicy.KEYWORD.equals(base.getRetrievalMode()) || base.getEmbeddingConnectionId() != null) {
            requireEmbedding(base);
        } else if (base.getDimensions() != null) {
            throw new ApiRuntimeException("设置维度时必须选择向量模型");
        }
        if (Boolean.TRUE.equals(base.getRerankEnabled())) {
            models.validate(base.getRerankConnectionId(), ModelType.RERANK, null);
        } else {
            base.setRerankConnectionId(null);
            base.setRerankThreshold(null);
        }
        int available = switch (base.getRetrievalMode()) {
            case KnowledgeRetrievalPolicy.KEYWORD -> base.getKeywordRecallSize();
            case KnowledgeRetrievalPolicy.VECTOR -> base.getVectorRecallSize();
            default -> base.getKeywordRecallSize() + base.getVectorRecallSize();
        };
        if (base.getTopK() > available) {
            throw new ApiRuntimeException("最终Top K不能大于候选召回数量");
        }
    }
}
