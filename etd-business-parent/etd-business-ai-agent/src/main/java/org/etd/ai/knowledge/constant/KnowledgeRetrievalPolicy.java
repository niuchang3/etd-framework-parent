package org.etd.ai.knowledge.constant;

import java.util.Set;
import org.etd.framework.common.core.exception.ApiRuntimeException;

/** 知识库检索模式、构建状态与受控物理表映射。 */
public final class KnowledgeRetrievalPolicy {
    /** 全文检索。 */
    public static final String KEYWORD = "keyword";
    /** 向量检索。 */
    public static final String VECTOR = "vector";
    /** 混合检索。 */
    public static final String HYBRID = "hybrid";
    /** 尚未构建。 */
    public static final String IDLE = "idle";
    /** 正在构建。 */
    public static final String BUILDING = "building";
    /** 最近构建成功。 */
    public static final String READY = "ready";
    /** 最近构建失败。 */
    public static final String FAILED = "failed";
    /** 系统支持维度。 */
    public static final Set<Integer> DIMENSIONS = Set.of(768, 1024, 1536);
    /** RRF平滑常量，避免比较不同检索源原始分数。 */
    public static final int RRF_CONSTANT = 60;
    /** 模型构建超时恢复窗口，单位秒。 */
    public static final int BUILD_TIMEOUT_SECONDS = 3600;
    private KnowledgeRetrievalPolicy() { }
    /** @param dimensions 已校验维度 @return 白名单内物理表名，不接受用户传入SQL标识符 */
    public static String vectorTable(Integer dimensions) {
        if (dimensions == null || !DIMENSIONS.contains(dimensions)) {
            throw new ApiRuntimeException("仅支持768、1024、1536维向量");
        }
        return "ai_vector_" + dimensions;
    }
}
