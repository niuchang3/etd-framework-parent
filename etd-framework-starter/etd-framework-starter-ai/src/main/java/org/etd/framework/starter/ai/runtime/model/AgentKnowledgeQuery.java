package org.etd.framework.starter.ai.runtime.model;

import java.util.List;

/** 框架交给业务的知识检索参数，业务不需要了解模型调用或追问重写过程。
 * @param knowledgeBaseIds 本轮绑定的知识库ID，结果分组顺序必须与此顺序一致
 * @param question 本轮实际检索词，必要时已由框架结合历史重写
 */
public record AgentKnowledgeQuery(List<Long> knowledgeBaseIds, String question) {
    public AgentKnowledgeQuery {
        knowledgeBaseIds = List.copyOf(knowledgeBaseIds);
    }
}
