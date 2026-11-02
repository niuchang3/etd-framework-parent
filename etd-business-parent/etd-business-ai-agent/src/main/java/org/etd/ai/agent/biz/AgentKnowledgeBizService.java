package org.etd.ai.agent.biz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.converter.AgentChatConverter;
import org.etd.ai.knowledge.biz.KnowledgeRecallBizService;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;
import org.springframework.ai.document.Document;
import org.etd.framework.starter.ai.memory.semantic.EmbeddingSpace;
import org.springframework.stereotype.Service;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeQueryService;

/**
 * 业务知识检索服务实现：负责资料授权、单库召回与编译期数据转换。
 */
@Service
@RequiredArgsConstructor
public class AgentKnowledgeBizService implements AgentKnowledgeQueryService {
    private final KnowledgeRecallBizService knowledge;
    private final KnowledgeBaseService bases;

    /** 工具发现和执行前均进行数据权限及启用状态校验，不能用已绑定或缓存结果代替。 */
    @Override
    public void requireAuthorized(List<Long> baseIds) {
        for (Long id : baseIds) {
            KnowledgeBaseEntity base = bases.requireBase(id, false, false);
            if (!Integer.valueOf(BasicConstant.DataStatus.ENABLED_CODE).equals(base.getDataStatus())) {
                throw new ApiRuntimeException("知识库已停用");
            }
        }
    }

    /** 框架恢复单库任务的可信上下文后调用，业务只负责权限、召回及数据映射。 */
    @Override
    public List<Document> queryKnowledge(Long baseId, String question, Map<EmbeddingSpace, float[]> vectors) {
        List<KnowledgeRecallHit> hits = knowledge.recallEnabled(baseId, question, vectors);
        List<Document> fragments = new ArrayList<>();
        for (KnowledgeRecallHit hit : hits) {
            Document fragment = AgentChatConverter.INSTANCE.toDocument(hit, baseId);
            fragments.add(fragment);
        }
        return List.copyOf(fragments);
    }
}
