package org.etd.framework.starter.ai.runtime.spi;

import java.util.List;
import org.etd.framework.starter.ai.runtime.model.KnowledgeFragment;

/** 知识检索扩展边界；实现方负责维度、权限、状态和内容版本校验。 */
@FunctionalInterface
public interface AgentKnowledgeQueryService {
    /** @param query 用户问题 @return 各知识库独立排序的候选列表 @throws RuntimeException 检索或授权失败 */
    List<List<KnowledgeFragment>> queryKnowledgeList(String query);
}
