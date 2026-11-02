package org.etd.framework.starter.ai.memory.semantic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot.Scope;
import org.etd.framework.starter.ai.document.tokenizer.DocumentTokenCounter;
import org.springframework.ai.document.Document;

/**
 * 长期语义记忆管理：负责多知识库候选结果的 RRF 排名融合与引用上下文生成。
 */
public final class AgentSemanticMemory {
    private static final int RRF_OFFSET = 60;
    private static final int MESSAGE_OVERHEAD = 16;

    /**
     * 框架以明确数据调用业务单库能力，承担多库并发、上下文和取消，不接收 Function 执行流程。
     * @param ids 已绑定范围 @param query 检索词 @param knowledge 业务授权与单库读取能力
     * @return 保持输入顺序的各库候选
     */
    public List<List<Document>> queryKnowledgeList(List<Long> ids, String query, AgentKnowledgeQueryService knowledge) {
        Map<EmbeddingSpace, float[]> vectors = new ConcurrentHashMap<>();
        RequestContextSnapshot context = RequestContextSnapshot.capture();
        List<Future<List<Document>>> tasks = new ArrayList<>();
        Map<Future<List<Document>>, Integer> positions = new LinkedHashMap<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            ExecutorCompletionService<List<Document>> completed = new ExecutorCompletionService<>(executor);
            try {
                for (Long id : ids) {
                    Future<List<Document>> task = completed.submit(() -> queryBase(id, query, knowledge, vectors, context));
                    positions.put(task, tasks.size());
                    tasks.add(task);
                }
                return awaitKnowledge(completed, positions);
            } finally {
                for (Future<List<Document>> task : tasks) {
                    task.cancel(true);
                }
            }
        }
    }

    private List<Document> queryBase(Long id, String query, AgentKnowledgeQueryService knowledge,
            Map<EmbeddingSpace, float[]> vectors, RequestContextSnapshot context) {
        try (Scope scope = context.openScope()) {
            return knowledge.queryKnowledge(id, query, vectors);
        }
    }

    private List<List<Document>> awaitKnowledge(ExecutorCompletionService<List<Document>> completed,
            Map<Future<List<Document>>, Integer> positions) {
        List<Document> empty = List.of();
        List<List<Document>> placeholders = Collections.nCopies(positions.size(), empty);
        List<List<Document>> result = new ArrayList<>(placeholders);
        try {
            // 按完成顺序发现权限或基础设施失败，同时按原始库顺序返回结果。
            for (int index = 0; index < positions.size(); index++) {
                Future<List<Document>> task = completed.take();
                List<Document> fragments = task.get();
                result.set(positions.get(task), fragments);
            }
            return List.copyOf(result);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("知识检索等待已取消", failure);
        } catch (ExecutionException failure) {
            if (failure.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw new IllegalStateException("知识检索失败", failure.getCause());
        }
    }

    /**
     * 将多个独立知识库返回的分组切片通过 RRF 平滑算法进行融合去重。
     * @param groups 各知识库的候选列表
     * @return 按综合得分降序排序的知识切片列表
     */
    public List<Document> fuseRankings(List<List<Document>> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        Map<String, Document> fragments = new LinkedHashMap<>();
        Map<String, Double> scores = new LinkedHashMap<>();
        for (List<Document> group : groups) {
            for (int index = 0; index < group.size(); index++) {
                Document fragment = group.get(index);
                fragments.putIfAbsent(fragment.getId(), fragment);
                scores.merge(fragment.getId(), 1.0 / (RRF_OFFSET + index + 1), Double::sum);
            }
        }
        ArrayList<Document> result = new ArrayList<>(fragments.values());
        result.sort(Comparator.<Document>comparingDouble(value -> scores.get(value.getId())).reversed()
                .thenComparing(Document::getId));
        return List.copyOf(result);
    }

    /**
     * 将融合后的参考片段按预算装配为知识引用文本。
     */
    public int appendKnowledge(List<Document> ranked, int maxReferences,
            List<Document> references, StringBuilder text, int remainingBudget) {
        int budget = remainingBudget - countTokens("以下为已授权的知识参考片段：");
        for (Document fragment : ranked) {
            if (references.size() >= maxReferences) {
                break;
            }
            String section = "[" + (references.size() + 1) + "]\n" + fragment.getText() + "\n\n";
            int cost = countTokens(section);
            if (cost <= budget) {
                references.add(fragment);
                text.append(section);
                budget -= cost;
            }
        }
        return Math.max(0, budget);
    }

    public int countTokens(String text) {
        return text == null || text.isEmpty() ? 0 : DocumentTokenCounter.count(text) + MESSAGE_OVERHEAD;
    }
}
