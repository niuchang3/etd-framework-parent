package org.etd.ai.knowledge.constant;

import java.util.Set;

/** 知识库保留配置使用的检索模式与支持维度。 */
public final class KnowledgeRetrievalPolicy {
    /** 检索问题字符数上限，HTTP和业务内部调用采用同一限制。 */
    public static final int MAX_QUERY_CHARACTERS = 2000;
    /** 全文检索。 */
    public static final String KEYWORD = "keyword";
    /** 向量检索。 */
    public static final String VECTOR = "vector";
    /** 混合检索。 */
    public static final String HYBRID = "hybrid";
    /** 系统支持维度。 */
    public static final Set<Integer> DIMENSIONS = Set.of(768, 1024, 1536);
    private KnowledgeRetrievalPolicy() { }
}
