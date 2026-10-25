package org.etd.ai.knowledge.biz;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.service.KnowledgeModelSettingsService;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.converter.KnowledgeBaseConverter;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.model.constant.ModelType;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
/** 知识库配置维护用例，仅保存并校验模型及检索参数。 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseBizService {
    private final KnowledgeBaseService bases;
    private final KnowledgeFolderService folders;
    private final KnowledgeModelSettingsService models;
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
        if (!Objects.equals(base.getOrgId(), input.getOrgId())) {
            throw new ApiRuntimeException("知识库创建后不允许变更组织归属");
        }
        validateSettings(input);
        KnowledgeBaseConverter.INSTANCE.updateEntity(input, base);
        bases.modifyBase(base);
    }
    /** @param id 已授权的空知识库 */
    @Transactional
    public void removeBase(Long id) {
        folders.lockStructure();
        bases.requireBase(id, true, true);
        bases.removeBase(id);
        folders.removeFolderList(id);
    }
    private void requireVersion(KnowledgeBaseEntity base, Long version) {
        if (!Objects.equals(base.getVersion(), version)) {
            throw new ApiRuntimeException("知识库已变化，请刷新后重试");
        }
    }
    private void requireEmbedding(KnowledgeBaseEntity base) {
        if (base.getDimensions() == null || !KnowledgeRetrievalPolicy.DIMENSIONS.contains(base.getDimensions())) {
            throw new ApiRuntimeException("仅支持768、1024、1536维向量");
        }
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
