package org.etd.ai.knowledge.biz;

import java.util.ArrayList;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeRecallService;
import org.etd.ai.knowledge.service.KnowledgeModelSettingsService;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.service.ModelInferenceService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

/** 知识库检索用例，复用同一路径提供正式业务召回与管理页召回测试。 */
@Service
@RequiredArgsConstructor
public class KnowledgeRecallBizService {
    private final KnowledgeBaseService bases;
    private final KnowledgeRecallService recall;
    private final KnowledgeModelSettingsService settings;
    private final ModelInferenceService inference;

    /** @param id 知识库 @param query 问题 @return 可读的已确认资料，包含尚未启用资料 */
    public List<KnowledgeRecallHit> testRecall(Long id, String query) {
        return recallList(id, query, true);
    }

    /** @param id 知识库 @param query 问题 @return 已启用且可读的已确认资料，供业务调用 */
    public List<KnowledgeRecallHit> recallList(Long id, String query) {
        return recallList(id, query, false);
    }

    /** 查询及重排期间不持有数据库事务，最后再次检查当前权限、配置及资料轮次。 */
    private List<KnowledgeRecallHit> recallList(Long id, String query, boolean test) {
        if (query == null || query.isBlank() || query.length() > KnowledgeRetrievalPolicy.MAX_QUERY_CHARACTERS) {
            throw new ApiRuntimeException("检索问题不能为空且不能超过2000个字符");
        }
        var base = bases.requireBase(id, false, false);
        String text = query.trim();
        var keyword = KnowledgeRetrievalPolicy.VECTOR.equals(base.getRetrievalMode())
                ? List.<KnowledgeRecallHit>of() : recall.fetchKeywordList(base, text, test);
        var vector = fetchVectorList(base, text, test);
        boolean hybrid = KnowledgeRetrievalPolicy.HYBRID.equals(base.getRetrievalMode());
        var candidates = recall.mergeCandidateList(keyword, vector, hybrid);
        if (Boolean.TRUE.equals(base.getRerankEnabled()) && !candidates.isEmpty()) {
            candidates = rerankCandidateList(base, text, candidates);
        }
        var latest = bases.requireBase(id, false, false);
        if (!Objects.equals(base.getVersion(), latest.getVersion())) {
            throw new ApiRuntimeException("知识库配置已变化，请重新检索");
        }
        var current = recall.retainCurrentList(id, candidates, test);
        var selected = current.subList(0, Math.min(base.getTopK(), current.size()));
        return List.copyOf(selected);
    }

    private List<KnowledgeRecallHit> fetchVectorList(KnowledgeBaseEntity base, String query, boolean test) {
        if (KnowledgeRetrievalPolicy.KEYWORD.equals(base.getRetrievalMode())) {
            return List.of();
        }
        if (base.getActiveIndexVersion() == 0
                || !Objects.equals(base.getEmbeddingConnectionId(), base.getActiveConnectionId())
                || !Objects.equals(base.getDimensions(), base.getActiveDimensions())) {
            throw new ApiRuntimeException("当前向量配置尚未建立索引，请先确认资料分片");
        }
        settings.validate(base.getActiveConnectionId(), ModelType.EMBEDDING, base.getActiveDimensions());
        var questions = List.of(query);
        var vectors = inference.createEmbeddingList(base.getActiveConnectionId(), base.getActiveDimensions(), questions);
        return recall.fetchVectorList(base, vectors.getFirst(), test);
    }

    /** SDK结果通过稳定ID匹配，禁止使用服务商返回正文替代数据库权威正文。 */
    private List<KnowledgeRecallHit> rerankCandidateList(KnowledgeBaseEntity base, String query,
            List<KnowledgeRecallHit> candidates) {
        settings.validate(base.getRerankConnectionId(), ModelType.RERANK, null);
        Map<String, KnowledgeRecallHit> byId = new HashMap<>();
        List<Document> documents = new ArrayList<>();
        for (var candidate : candidates) {
            String key = candidate.getChunkId().toString();
            var document = Document.builder().id(key).text(candidate.getContent()).build();
            byId.put(key, candidate);
            documents.add(document);
        }
        var results = inference.createRerankList(base.getRerankConnectionId(), query, documents);
        return selectRerankedList(base, results, byId);
    }

    /** 阈值应用于模型原始重排分数，未知ID或重复ID作为无效响应明确拒绝。 */
    private List<KnowledgeRecallHit> selectRerankedList(KnowledgeBaseEntity base,
            List<DocumentWithScore> results, Map<String, KnowledgeRecallHit> byId) {
        List<KnowledgeRecallHit> reranked = new ArrayList<>();
        for (var result : results) {
            var hit = result.getOutput() == null ? null : byId.remove(result.getOutput().getId());
            Double score = result.getScore();
            if (hit == null || score == null || !Double.isFinite(score)) {
                throw new ApiRuntimeException("重排响应无效，请检查模型服务");
            }
            if (base.getRerankThreshold() == null || score >= base.getRerankThreshold()) {
                hit.setRerankScore(score);
                hit.setScore(score);
                reranked.add(hit);
            }
        }
        reranked.sort(Comparator.comparing(KnowledgeRecallHit::getScore).reversed()
                .thenComparing(KnowledgeRecallHit::getChunkId));
        return reranked;
    }
}
