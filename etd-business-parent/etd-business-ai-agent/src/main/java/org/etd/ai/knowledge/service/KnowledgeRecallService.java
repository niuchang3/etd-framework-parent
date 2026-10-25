package org.etd.ai.knowledge.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.mapper.KnowledgeRecallMapper;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import org.springframework.stereotype.Service;

/** 召回查询及排名融合能力，所有正文关联均在SQL完成。 */
@Service
@RequiredArgsConstructor
@DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_base", alias = "permission_base"))
public class KnowledgeRecallService {
    /** RRF常用平滑常数，按排名融合，避免直接相加不同量纲的全文与余弦分数。 */
    private static final int RRF_OFFSET = 60;
    private final KnowledgeRecallMapper mapper;

    /** @param base 已授权知识库 @param query 问题 @param test 是否召回未启用资料 @return 全文候选 */
    public List<KnowledgeRecallHit> fetchKeywordList(KnowledgeBaseEntity base, String query, boolean test) {
        return mapper.selectKeywordList(base.getId(), query, base.getKeywordRecallSize(), test);
    }

    /** @param base 已授权且已建立向量空间的知识库 @param vector 问题向量 @param test 是否允许禁用资料
     * @return 当前代次且内容轮次有效的向量候选 */
    public List<KnowledgeRecallHit> fetchVectorList(KnowledgeBaseEntity base, float[] vector, boolean test) {
        String table = KnowledgeVectorService.resolveTable(base.getActiveDimensions());
        String value = KnowledgeVectorService.formatVector(vector);
        return mapper.selectVectorList(base.getId(), table, value, base.getActiveConnectionId(),
                base.getActiveIndexVersion(), base.getSimilarityThreshold(), base.getVectorRecallSize(), test);
    }

    /** @param keyword 全文候选 @param vector 向量候选 @param hybrid 是否混合排名 @return 去重后的有序候选 */
    public List<KnowledgeRecallHit> mergeCandidateList(List<KnowledgeRecallHit> keyword,
            List<KnowledgeRecallHit> vector, boolean hybrid) {
        Map<Long, KnowledgeRecallHit> merged = new LinkedHashMap<>();
        appendCandidateList(merged, keyword, hybrid, true);
        appendCandidateList(merged, vector, hybrid, false);
        var result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(KnowledgeRecallHit::getScore).reversed()
                .thenComparing(KnowledgeRecallHit::getChunkId));
        return result;
    }

    /** 这里只合并两路检索排名，不在Java组装任何业务表关联。 */
    private void appendCandidateList(Map<Long, KnowledgeRecallHit> merged, List<KnowledgeRecallHit> source,
            boolean hybrid, boolean keyword) {
        for (int index = 0; index < source.size(); index++) {
            var hit = source.get(index);
            var target = merged.get(hit.getChunkId());
            double score = hybrid ? 1.0 / (RRF_OFFSET + index + 1)
                    : keyword ? hit.getKeywordScore() : hit.getVectorScore();
            if (target == null) {
                hit.setScore(score);
                merged.put(hit.getChunkId(), hit);
            } else {
                target.setVectorScore(hit.getVectorScore());
                target.setScore(target.getScore() + score);
            }
        }
    }

    /**
     * @param baseId 当前知识库 @param candidates 模型调用前的候选 @param test 是否测试召回
     * @return 外部调用后仍有权限且内容轮次、正文未变化的候选，保持排序
     */
    public List<KnowledgeRecallHit> retainCurrentList(Long baseId, List<KnowledgeRecallHit> candidates, boolean test) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        var ids = candidates.stream().map(KnowledgeRecallHit::getChunkId).toList();
        var current = mapper.selectCurrentList(baseId, ids, test);
        Map<Long, KnowledgeRecallHit> readable = new LinkedHashMap<>();
        for (var hit : current) {
            readable.put(hit.getChunkId(), hit);
        }
        List<KnowledgeRecallHit> result = new ArrayList<>();
        for (var hit : candidates) {
            var latest = readable.get(hit.getChunkId());
            if (latest != null && Objects.equals(hit.getProcessingVersion(), latest.getProcessingVersion())
                    && Objects.equals(hit.getContent(), latest.getContent())) {
                hit.setDocumentName(latest.getDocumentName());
                result.add(hit);
            }
        }
        return result;
    }
}
