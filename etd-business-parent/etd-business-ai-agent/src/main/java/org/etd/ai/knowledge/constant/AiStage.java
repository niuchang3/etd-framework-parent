package org.etd.ai.knowledge.constant;

/** 本阶段实际使用的状态与参数边界，后续模型调用不得复用为已验证状态。 */
public final class AiStage {
    /** 文件仅完成存储。 */
    public static final String UPLOADED = "uploaded";

    /** 仅保存全文检索策略。 */
    public static final String KEYWORD = "keyword";

    /** 向量模型类型。 */
    public static final String EMBEDDING = "embedding";

    /** 默认分片Token上限。 */
    public static final int CHUNK_SIZE = 512;

    /** 默认重叠Token。 */
    public static final int OVERLAP = 64;

    /** 默认分支候选数。 */
    public static final int CANDIDATES = 20;

    /** 默认最终结果数。 */
    public static final int RESULTS = 5;

    private AiStage() {}
}
