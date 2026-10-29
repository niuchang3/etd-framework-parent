package org.etd.ai.knowledge.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.mapper.KnowledgeRecallMapper;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;
import org.etd.ai.knowledge.model.KnowledgeRecallHitComparator;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import org.springframework.stereotype.Service;

/** 召回查询及排名融合能力，所有正文关联均在SQL完成。 */
@Service
@RequiredArgsConstructor
// 召回以资料为授权边界；分片、向量通过SQL归属关联，不能因知识库公开而放行私有资料。
@DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_document", publicColumn = KnowledgePolicy.PUBLIC_COLUMN))
public class KnowledgeRecallService {
    /** RRF常用平滑常数，按排名融合，避免直接相加不同量纲的全文与余弦分数。 */
    private static final int RRF_OFFSET = 60;
    private final KnowledgeRecallMapper mapper;

    /** @param base 已授权知识库 @param query 问题 @param formal 是否仅允许正式启用资料 @return 全文候选 */
    public List<KnowledgeRecallHit> fetchKeywordList(KnowledgeBaseEntity base, String query, boolean formal) {
        return mapper.selectKeywordList(base.getId(), query, base.getKeywordRecallSize(), formal);
    }

    /** @param base 已授权且已建立向量空间的知识库 @param vector 问题向量 @param formal 是否仅允许正式启用资料
     * @return 当前代次且内容轮次有效的向量候选 */
    public List<KnowledgeRecallHit> fetchVectorList(KnowledgeBaseEntity base, float[] vector, boolean formal) {
        String table = KnowledgeVectorService.resolveTable(base.getActiveDimensions());
        String value = KnowledgeVectorService.formatVector(vector);
        return mapper.selectVectorList(base.getId(), table, value, base.getActiveConnectionId(),
                base.getActiveIndexVersion(), base.getSimilarityThreshold(), base.getVectorRecallSize(), formal);
    }

    /** @param keyword 全文候选 @param vector 向量候选 @param hybrid 是否混合排名 @return 去重后的有序候选 */
    public List<KnowledgeRecallHit> mergeCandidateList(List<KnowledgeRecallHit> keyword,
            List<KnowledgeRecallHit> vector, boolean hybrid) {
        Map<Long, KnowledgeRecallHit> merged = new LinkedHashMap<>();
        if (hybrid) {
            appendCandidateList(merged, keyword, KnowledgeRetrievalPolicy.HYBRID);
            appendCandidateList(merged, vector, KnowledgeRetrievalPolicy.HYBRID);
        } else {
            appendCandidateList(merged, keyword, KnowledgeRetrievalPolicy.KEYWORD);
            appendCandidateList(merged, vector, KnowledgeRetrievalPolicy.VECTOR);
        }
        ArrayList<KnowledgeRecallHit> result = new ArrayList<>(merged.values());
        result.sort(KnowledgeRecallHitComparator.INSTANCE);
        return result;
    }

    /** 这里只合并两路检索排名，不在Java组装任何业务表关联。 */
    private void appendCandidateList(Map<Long, KnowledgeRecallHit> merged, List<KnowledgeRecallHit> source,
            String scoringMode) {
        for (int index = 0; index < source.size(); index++) {
            KnowledgeRecallHit hit = source.get(index);
            KnowledgeRecallHit target = merged.get(hit.getChunkId());
            double score = calculateCandidateScore(hit, index, scoringMode);
            if (target == null) {
                hit.setScore(score);
                merged.put(hit.getChunkId(), hit);
            } else {
                target.setVectorScore(hit.getVectorScore());
                target.setScore(target.getScore() + score);
            }
        }
    }

    /** 混合检索按名次融合，单路检索保留其原始分数，不能把全文分数与余弦分数直接比较。 */
    private double calculateCandidateScore(KnowledgeRecallHit hit, int index, String scoringMode) {
        return switch (scoringMode) {
            case KnowledgeRetrievalPolicy.HYBRID -> 1.0 / (RRF_OFFSET + index + 1);
            case KnowledgeRetrievalPolicy.KEYWORD -> hit.getKeywordScore();
            case KnowledgeRetrievalPolicy.VECTOR -> hit.getVectorScore();
            default -> throw new IllegalArgumentException("不支持的召回评分方式");
        };
    }

    /**
     * @param baseId 当前知识库 @param candidates 模型调用前的候选 @param formal 是否仅允许正式启用资料
     * @return 外部调用后仍有权限且内容轮次、正文未变化的候选，保持排序
     */
    public List<KnowledgeRecallHit> retainCurrentList(Long baseId, List<KnowledgeRecallHit> candidates, boolean formal) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        List<Long> ids = candidates.stream().map(KnowledgeRecallHit::getChunkId).toList();
        List<KnowledgeRecallHit> current = mapper.selectCurrentList(baseId, ids, formal);
        Map<Long, KnowledgeRecallHit> readable = new LinkedHashMap<>();
        for (KnowledgeRecallHit hit : current) {
            readable.put(hit.getChunkId(), hit);
        }
        List<KnowledgeRecallHit> result = new ArrayList<>();
        for (KnowledgeRecallHit hit : candidates) {
            KnowledgeRecallHit latest = readable.get(hit.getChunkId());
            if (latest != null && Objects.equals(hit.getProcessingVersion(), latest.getProcessingVersion())
                    && Objects.equals(hit.getContent(), latest.getContent())) {
                hit.setDocumentName(latest.getDocumentName());
                result.add(hit);
            }
        }
        return result;
    }

    /**
     * 指针级滑动扩展：根据 (document_id, chunk_index) 逻辑指针动态平滑拼接前后相邻分片。
     * @param hits 已通过权限核验并排序的候选分片
     * @param formal 是否仅允许正式启用资料
     * @param windowSize 窗口大小，0或空表示不扩展，1表示前后各扩展1片
     * @return 扩展后的分片列表副本
     */
    public List<KnowledgeRecallHit> expandContextWindow(List<KnowledgeRecallHit> hits, Integer windowSize, boolean formal) {
        if (hits == null || hits.isEmpty() || windowSize == null || windowSize <= 0) {
            return hits == null ? List.of() : hits;
        }
        List<KnowledgeRecallHit> expandedHits = new ArrayList<>(hits.size());
        for (KnowledgeRecallHit hit : hits) {
            if (hit.getDocumentId() == null || hit.getChunkIndex() == null) {
                expandedHits.add(hit);
                continue;
            }
            int minIdx = Math.max(0, hit.getChunkIndex() - windowSize);
            int maxIdx = hit.getChunkIndex() + windowSize;
            // 扩展时重新关联资料并执行同一框架权限规则，同时拒绝已变化的正文轮次。
            List<KnowledgeRecallHit> neighbors = mapper.selectContextList(hit.getDocumentId(), hit.getProcessingVersion(), minIdx, maxIdx, formal);
            if (neighbors.isEmpty()) {
                continue;
            }
            hit.setContent(mergeContextContent(neighbors));
            expandedHits.add(hit);
        }
        return expandedHits;
    }

    /** 只拼接本次SQL重新授权的相邻正文，不拼入旧候选快照中的文本。 */
    private String mergeContextContent(List<KnowledgeRecallHit> neighbors) {
        StringBuilder content = new StringBuilder();
        for (KnowledgeRecallHit neighbor : neighbors) {
            if (neighbor.getContent() != null && !neighbor.getContent().isBlank()) {
                if (!content.isEmpty()) {
                    content.append("\n\n");
                }
                content.append(neighbor.getContent());
            }
        }
        return content.toString();
    }

}
