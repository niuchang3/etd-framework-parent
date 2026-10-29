package org.etd.ai.knowledge.model;

import java.util.Comparator;

/** 召回合并和模型重排共用的排序规则：分数降序，同分时按分片ID数值升序。 */
public final class KnowledgeRecallHitComparator implements Comparator<KnowledgeRecallHit> {
    /** 无状态比较器，排序规则只维护一份。 */
    public static final KnowledgeRecallHitComparator INSTANCE = new KnowledgeRecallHitComparator();

    private KnowledgeRecallHitComparator() {
    }

    /** @param first 第一个候选 @param second 第二个候选 @return 分数高的在前，同分时ID小的在前 */
    @Override
    public int compare(KnowledgeRecallHit first, KnowledgeRecallHit second) {
        int scoreOrder = Double.compare(second.getScore(), first.getScore());
        if (scoreOrder != 0) {
            return scoreOrder;
        }
        return Long.compare(first.getChunkId(), second.getChunkId());
    }
}
