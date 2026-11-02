package org.etd.framework.starter.ai.memory.semantic;

import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;

/** 可选的业务知识访问能力，不要求纯模型 Agent 实现。 */
public interface AgentKnowledgeQueryService {
    /**
     * 发现知识工具及每次执行前复核绑定范围；不检索、不调用模型、不缓存授权结论。
     * @param baseIds Agent 已绑定的知识库标识
     * @throws RuntimeException 当前身份不可访问或知识库停用
     */
    void requireAuthorized(List<Long> baseIds);

    /**
     * 框架在恢复可信身份的异步线程中调用；每次检索必须复核访问权限与当前索引版本。
     * 不开启覆盖远程模型调用的长事务；异常明确传播，不用缓存命中替代授权。
     * 同一 Run 的多库读取可能并发且可能取消，实现不得跨请求复用向量缓存，并应响应中断。
     * @param baseId Agent 已绑定的单个知识库，不能由模型扩大
     * @param vectors 本轮共享的查询向量数据，按连接和维度隔离，不跨请求保存
     * @param question 本轮检索词
     * @return 单库候选排名，框架负责多库并发、融合与预算选择
     * @throws RuntimeException 访问被拒绝、配置失效或检索失败
     */
    List<Document> queryKnowledge(Long baseId, String question, Map<EmbeddingSpace, float[]> vectors);
}
