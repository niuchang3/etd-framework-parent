package org.etd.ai.agent.biz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.converter.AgentChatConverter;
import org.etd.ai.agent.model.AgentCitation;
import org.etd.ai.knowledge.biz.KnowledgeRecallBizService;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;
import org.etd.framework.starter.ai.runtime.model.AgentKnowledgeQuery;
import org.etd.framework.starter.ai.runtime.model.KnowledgeFragment;
import org.etd.framework.starter.ai.runtime.spi.AgentKnowledgeQueryService;
import org.springframework.stereotype.Service;

/** 业务知识检索接口实现，只负责资料授权、召回和引用来源，不组织模型或执行器。 */
@Service
@RequiredArgsConstructor
public class AgentKnowledgeBizService implements AgentKnowledgeQueryService {
    private final KnowledgeRecallBizService knowledge;

    /** @param query 框架提供的知识库和实际检索词 @return 各知识库的有序候选及业务引用
     * @throws RuntimeException 权限或召回失败，由框架保存本轮失败状态
     */
    @Override
    public List<List<KnowledgeFragment>> queryKnowledgeList(AgentKnowledgeQuery query) {
        List<Long> ids = query.knowledgeBaseIds();
        List<List<KnowledgeRecallHit>> groups = knowledge.recallEnabledList(ids, query.question());
        List<List<KnowledgeFragment>> result = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            List<KnowledgeFragment> fragments = new ArrayList<>();
            for (KnowledgeRecallHit hit : groups.get(index)) {
                AgentCitation citation = AgentChatConverter.INSTANCE.toCitation(hit, ids.get(index));
                Map<String, Object> metadata = Map.of(AgentChatPolicy.CITATION, citation);
                KnowledgeFragment fragment = new KnowledgeFragment(hit.getChunkId().toString(), hit.getContent(), metadata);
                fragments.add(fragment);
            }
            result.add(fragments);
        }
        return result;
    }
}
