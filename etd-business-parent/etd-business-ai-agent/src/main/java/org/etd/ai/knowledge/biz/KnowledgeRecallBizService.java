package org.etd.ai.knowledge.biz;

import java.util.ArrayList;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;
import org.etd.ai.knowledge.model.KnowledgeRecallHitComparator;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeRecallService;
import org.etd.ai.knowledge.service.KnowledgeModelSettingsService;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.service.ModelInferenceService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.memory.semantic.EmbeddingSpace;
import org.springframework.ai.document.Document;
import org.etd.framework.common.core.constants.BasicConstant;
import org.springframework.stereotype.Service;

/** 知识库管理页召回测试用例，允许检索尚未启用的已确认资料。 */
@Service
@RequiredArgsConstructor
public class KnowledgeRecallBizService {
    private final KnowledgeBaseService bases;
    private final KnowledgeRecallService recall;
    private final KnowledgeModelSettingsService settings;
    private final ModelInferenceService inference;

    /**
     * 查询及重排期间不持有数据库事务，最后复核当前权限、配置及资料轮次。
     * @param id 知识库
     * @param query 问题
     * @return 可读的已确认资料，包含尚未启用资料
     * @throws ApiRuntimeException 问题无效、无读取权限、配置变化或模型不可用
     */
    public List<KnowledgeRecallHit> testRecall(Long id, String query) {
        Map<EmbeddingSpace, float[]> vectors = new HashMap<>();
        return recallBase(id, query, false, vectors);
    }

    /** @param ids 业务批量读取范围 @param query 问题 @return 各库独立排名，业务层不组织线程或传递执行回调 */
    public List<List<KnowledgeRecallHit>> recallEnabledList(List<Long> ids, String query) {
        Map<EmbeddingSpace, float[]> vectors = new HashMap<>();
        List<List<KnowledgeRecallHit>> result = new ArrayList<>();
        if (ids != null) {
            for (Long id : ids) {
                List<KnowledgeRecallHit> hits = recallEnabled(id, query, vectors);
                result.add(hits);
            }
        }
        return List.copyOf(result);
    }

    /** @param id 单库标识 @param query 问题 @param vectors 框架管理生命周期的本轮向量数据 @return 已授权当前版本候选 */
    public List<KnowledgeRecallHit> recallEnabled(Long id, String query, Map<EmbeddingSpace, float[]> vectors) {
        return recallBase(id, query, true, vectors);
    }

    /** 每轮缓存仅复用相同模型连接及维度的问题向量，不跨请求缓存身份或查询内容。 */
    private List<KnowledgeRecallHit> recallBase(Long id, String query, boolean formal,
            Map<EmbeddingSpace, float[]> vectors) {
        if (query == null || query.isBlank() || query.length() > KnowledgeRetrievalPolicy.MAX_QUERY_CHARACTERS) {
            throw new ApiRuntimeException("检索问题不能为空且不能超过2000个字符");
        }
        KnowledgeBaseEntity base = bases.requireBase(id, false, false);
        requireEnabled(base, formal);
        String text = query.trim();
        List<KnowledgeRecallHit> keyword = KnowledgeRetrievalPolicy.VECTOR.equals(base.getRetrievalMode())
                ? List.<KnowledgeRecallHit>of() : recall.fetchKeywordList(base, text, formal);
        List<KnowledgeRecallHit> vector = fetchVectorList(base, text, formal, vectors);
        boolean hybrid = KnowledgeRetrievalPolicy.HYBRID.equals(base.getRetrievalMode());
        List<KnowledgeRecallHit> candidates = recall.mergeCandidateList(keyword, vector, hybrid);
        if (Boolean.TRUE.equals(base.getRerankEnabled()) && !candidates.isEmpty()) {
            candidates = rerankCandidateList(base, text, candidates);
        }
        KnowledgeBaseEntity latest = bases.requireBase(id, false, false);
        requireEnabled(latest, formal);
        if (!Objects.equals(base.getVersion(), latest.getVersion())) {
            throw new ApiRuntimeException("知识库配置已变化，请重新检索");
        }
        List<KnowledgeRecallHit> current = recall.retainCurrentList(id, candidates, formal);
        List<KnowledgeRecallHit> selected = current.subList(0, Math.min(base.getTopK(), current.size()));
        List<KnowledgeRecallHit> expanded = recall.expandContextWindow(selected, base.getContextWindowSize(), formal);
        return List.copyOf(expanded);
    }

    private List<KnowledgeRecallHit> fetchVectorList(KnowledgeBaseEntity base, String query, boolean formal,
            Map<EmbeddingSpace, float[]> cache) {
        if (KnowledgeRetrievalPolicy.KEYWORD.equals(base.getRetrievalMode())) {
            return List.of();
        }
        if (base.getActiveIndexVersion() == 0
                || !Objects.equals(base.getEmbeddingConnectionId(), base.getActiveConnectionId())
                || !Objects.equals(base.getDimensions(), base.getActiveDimensions())) {
            throw new ApiRuntimeException("当前向量配置尚未建立索引，请先确认资料分片");
        }
        settings.validate(base.getActiveConnectionId(), ModelType.EMBEDDING, base.getActiveDimensions());
        EmbeddingSpace space = new EmbeddingSpace(base.getActiveConnectionId(), base.getActiveDimensions());
        float[] vector;
        // 同一 Run 的向量缓存只在生成期间协调；避免多库命中同一空间时重复计费，不能替代上面的授权。
        synchronized (cache) {
            vector = cache.get(space);
            if (vector == null) {
                List<String> questions = List.of(query);
                List<float[]> generated = inference.createEmbeddingList(space.connectionId(), space.dimensions(), questions);
                vector = generated.getFirst();
                cache.put(space, vector);
            }
        }
        return recall.fetchVectorList(base, vector, formal);
    }

    private void requireEnabled(KnowledgeBaseEntity base, boolean formal) {
        if (formal && !Objects.equals(base.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("知识库已停用，不能用于正式问答");
        }
    }

    /** SDK结果通过稳定ID匹配，禁止使用服务商返回正文替代数据库权威正文。 */
    private List<KnowledgeRecallHit> rerankCandidateList(KnowledgeBaseEntity base, String query,
            List<KnowledgeRecallHit> candidates) {
        settings.validate(base.getRerankConnectionId(), ModelType.RERANK, null);
        Map<String, KnowledgeRecallHit> byId = new HashMap<>();
        List<Document> documents = new ArrayList<>();
        for (KnowledgeRecallHit candidate : candidates) {
            String key = candidate.getChunkId().toString();
            Document document = Document.builder().id(key).text(candidate.getContent()).build();
            byId.put(key, candidate);
            documents.add(document);
        }
        List<DocumentWithScore> results = inference.createRerankList(base.getRerankConnectionId(), query, documents);
        return selectRerankedList(base, results, byId);
    }

    /** 阈值应用于模型原始重排分数，未知ID或重复ID作为无效响应明确拒绝。 */
    private List<KnowledgeRecallHit> selectRerankedList(KnowledgeBaseEntity base,
            List<DocumentWithScore> results, Map<String, KnowledgeRecallHit> byId) {
        List<KnowledgeRecallHit> reranked = new ArrayList<>();
        for (DocumentWithScore result : results) {
            KnowledgeRecallHit hit = result.getOutput() == null ? null : byId.remove(result.getOutput().getId());
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
        reranked.sort(KnowledgeRecallHitComparator.INSTANCE);
        return reranked;
    }
}
