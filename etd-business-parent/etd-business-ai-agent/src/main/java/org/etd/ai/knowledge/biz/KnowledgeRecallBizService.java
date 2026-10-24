package org.etd.ai.knowledge.biz;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.component.KnowledgeModelClient;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.model.KnowledgeSearchHit;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeRetrievalService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import java.util.*;
/** 全文/向量/混合召回、RRF融合与可选重排；正式入口不可绕过文档启停。 */
@Service
@RequiredArgsConstructor
public class KnowledgeRecallBizService {
    private final KnowledgeBaseService bases;
    private final KnowledgeRetrievalService retrieval;
    private final KnowledgeModelClient models;
    /** @param id 库 @param query 问题 @param status 测试筛选 @param test 是否管理召回测试 @return 最终候选 */
    public List<KnowledgeSearchHit> recall(Long id, String query, Integer status, boolean test) {
        var base = bases.requireBase(id, test, false);
        if (!test && !Objects.equals(base.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("知识库已停用");
        }
        Integer filter = status;
        if (!test) {
            filter = BasicConstant.DataStatus.ENABLED_CODE;
        }
        List<KnowledgeSearchHit> keyword = List.of();
        List<KnowledgeSearchHit> vector = List.of();
        if (!KnowledgeRetrievalPolicy.VECTOR.equals(base.getRetrievalMode())) {
            keyword = retrieval.fetchKeywordList(base, query, filter);
        }
        if (!KnowledgeRetrievalPolicy.KEYWORD.equals(base.getRetrievalMode())) {
            if (base.getActiveConnectionId() == null || base.getActiveIndexVersion() == 0) {
                throw new ApiRuntimeException("请先完成向量索引初始化");
            }
            var embedding = models.embed(base.getActiveConnectionId(), base.getActiveDimensions(), query, true);
            String text = Arrays.toString(embedding);
            vector = retrieval.fetchVectorList(base, text, filter);
        }
        var candidates = fuseCandidateList(keyword, vector);
        if (Boolean.TRUE.equals(base.getRerankEnabled()) && !candidates.isEmpty()) {
            var texts = candidates.stream().map(KnowledgeSearchHit::getContent).toList();
            var scores = models.rerank(base.getRerankConnectionId(), query, texts);
            for (int index = 0; index < candidates.size(); index++) {
                candidates.get(index).setRerankScore(scores.get(index));
            }
            candidates.sort(Comparator.comparing(KnowledgeSearchHit::getRerankScore).reversed()
                    .thenComparing(KnowledgeSearchHit::getChunkId));
            if (base.getRerankThreshold() != null) {
                candidates.removeIf(hit -> hit.getRerankScore() < base.getRerankThreshold());
            }
        }
        return List.copyOf(candidates.subList(0, Math.min(candidates.size(), base.getTopK())));
    }
    /** 按分片ID去重，并按排名融合，绝不直接相加全文、余弦或重排原始分数。 */
    private List<KnowledgeSearchHit> fuseCandidateList(List<KnowledgeSearchHit> keyword, List<KnowledgeSearchHit> vector) {
        Map<Long, KnowledgeSearchHit> merged = new LinkedHashMap<>();
        mergeRanks(merged, keyword);
        mergeRanks(merged, vector);
        var result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(KnowledgeSearchHit::getFusionScore).reversed()
                .thenComparing(KnowledgeSearchHit::getChunkId));
        return result;
    }
    private void mergeRanks(Map<Long, KnowledgeSearchHit> merged, List<KnowledgeSearchHit> source) {
        for (int index = 0; index < source.size(); index++) {
            var candidate = source.get(index);
            var hit = merged.computeIfAbsent(candidate.getChunkId(), ignored -> candidate);
            if (candidate.getKeywordScore() != null) {
                hit.setKeywordScore(candidate.getKeywordScore());
            }
            if (candidate.getVectorScore() != null) {
                hit.setVectorScore(candidate.getVectorScore());
            }
            double previous = hit.getFusionScore() == null ? 0 : hit.getFusionScore();
            hit.setFusionScore(previous + 1.0 / (KnowledgeRetrievalPolicy.RRF_CONSTANT + index + 1));
        }
    }
}
