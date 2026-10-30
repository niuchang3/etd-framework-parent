package org.etd.framework.starter.ai.runtime.spi;

import java.util.List;
import org.etd.framework.starter.ai.runtime.model.KnowledgeFragment;
import org.etd.framework.starter.ai.runtime.model.AgentKnowledgeQuery;

/** 知识检索扩展边界；实现方负责维度、权限、状态和内容版本校验。 */
public interface AgentKnowledgeQueryService {
    /** 准备事务提交后由框架工作线程调用，本轮请求上下文已恢复；实现方无需处理线程或模型。
     * 实现方负责知识库、资料权限、维度及内容版本校验，外部推理期间不得持有长数据库事务。
     * 不同聊天可并发调用同一个Bean，调用被中断时应停止后续工作并保留中断标记。
     * @param query 本轮知识库及检索词 @return 按知识库顺序分组且各组内部已排序的候选
     * @throws RuntimeException 检索或授权失败，框架将结束本轮并保存失败状态
     */
    List<List<KnowledgeFragment>> queryKnowledgeList(AgentKnowledgeQuery query);
}
