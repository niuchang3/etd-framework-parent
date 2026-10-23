package org.etd.ai.knowledge.biz;

import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.*;
import org.etd.ai.knowledge.entity.*;
import org.etd.ai.knowledge.model.KnowledgeChunkSource;
import org.etd.ai.knowledge.service.KnowledgeChunkService;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.document.tokenizer.DocumentTokenCounter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 分片人工编辑、相邻合并和确认；以资料行锁和版本串行化整份结果的修改。 */
@Service
@RequiredArgsConstructor
public class KnowledgeChunkEditBizService {
    private final KnowledgeDocumentService documents;
    private final KnowledgeChunkService chunks;
    /** @param id 资料ID @param version 资料版本 @param edits 分片ID与新正文 @return 最新资料状态 */
    @Transactional
    public KnowledgeDocumentEntity modifyChunks(Long id, Long version, Map<Long, String> edits) {
        var document = requireEditable(id, version);
        for (var entry : edits.entrySet()) {
            var chunk = chunks.requireChunk(id, entry.getKey());
            chunk.setContent(entry.getValue());
            chunk.setTokenCount(DocumentTokenCounter.count(entry.getValue()));
            chunks.modifyChunk(chunk);
        }
        markDraft(document);
        return document;
    }

    /** @param id 资料ID @param version 资料版本 @param ids 按当前原文顺序排列的相邻分片ID @return 合并后的最新资料状态 */
    @Transactional
    public KnowledgeDocumentEntity mergeChunks(Long id, Long version, List<Long> ids) {
        var document = requireEditable(id, version);
        var records = chunks.fetchChunkList(id);
        List<KnowledgeChunkEntity> selected = requireAdjacent(records, ids);
        String content = mergeContent(selected);
        var source = mergeSource(selected);
        var first = selected.getFirst();
        first.setContent(content);
        first.setTokenCount(DocumentTokenCounter.count(content));
        first.setSource(source);
        chunks.modifyChunk(first);
        for (int index = 1; index < selected.size(); index++) { chunks.removeChunk(selected.get(index).getId()); }
        document.setChunkCount(records.size() - selected.size() + 1);
        markDraft(document);
        return document;
    }

    /** @param id 资料ID @param version 当前版本 @return 已确认状态；不执行任何向量化 */
    @Transactional
    public KnowledgeDocumentEntity confirmChunks(Long id, Long version) {
        var document = requireEditable(id, version);
        if (document.getChunkCount() == null || document.getChunkCount() == 0) { throw new ApiRuntimeException("没有可确认的分片"); }
        document.setChunkStatus(KnowledgeChunkStatus.CONFIRMED);
        document.setChunkConfirmedTime(Instant.now());
        documents.modifyDocument(document);
        return document;
    }

    private KnowledgeDocumentEntity requireEditable(Long id, Long version) {
        var document = documents.requireDocument(id, true);
        documents.requireChunkIdle(document);
        if (!Objects.equals(document.getVersion(), version)) { throw new ApiRuntimeException("分片已变化，请刷新后重试"); }
        if (document.getChunkCount() == null || document.getChunkCount() == 0) { throw new ApiRuntimeException("请先生成分片"); }
        return document;
    }

    private List<KnowledgeChunkEntity> requireAdjacent(List<KnowledgeChunkEntity> all, List<Long> ids) {
        Set<Long> unique = new HashSet<>(ids);
        if (ids.size() < 2 || unique.size() != ids.size()) { throw new ApiRuntimeException("至少选择两个不同的相邻分片"); }
        int first = -1;
        for (int index = 0; index < all.size(); index++) {
            if (all.get(index).getId().equals(ids.getFirst())) { first = index; break; }
        }
        if (first < 0 || first + ids.size() > all.size()) { throw new ApiRuntimeException("分片不属于当前资料"); }
        List<KnowledgeChunkEntity> selected = new ArrayList<>();
        for (int index = 0; index < ids.size(); index++) {
            var chunk = all.get(first + index);
            if (!chunk.getId().equals(ids.get(index))) { throw new ApiRuntimeException("只能按原文顺序合并相邻分片"); }
            selected.add(chunk);
        }
        return selected;
    }

    private String mergeContent(List<KnowledgeChunkEntity> selected) {
        StringBuilder content = new StringBuilder();
        for (KnowledgeChunkEntity chunk : selected) {
            if (!content.isEmpty()) { content.append(KnowledgeChunkLimits.CONTENT_SEPARATOR); }
            content.append(chunk.getContent());
        }
        if (content.length() > KnowledgeChunkLimits.MAX_CONTENT_CHARACTERS) { throw new ApiRuntimeException("合并后正文超过长度限制"); }
        return content.toString();
    }

    private KnowledgeChunkSource mergeSource(List<KnowledgeChunkEntity> selected) {
        Set<String> headings = new LinkedHashSet<>();
        Set<Integer> pages = new TreeSet<>();
        Set<String> refs = new LinkedHashSet<>();
        for (KnowledgeChunkEntity chunk : selected) {
            headings.addAll(chunk.getSource().headings());
            pages.addAll(chunk.getSource().pageNumbers());
            refs.addAll(chunk.getSource().documentItemRefs());
        }
        var path = List.copyOf(headings);
        var pageNumbers = List.copyOf(pages);
        var references = List.copyOf(refs);
        return new KnowledgeChunkSource(path, pageNumbers, references);
    }

    private void markDraft(KnowledgeDocumentEntity document) {
        document.setChunkStatus(KnowledgeChunkStatus.DRAFT);
        document.setChunkConfirmedTime(null);
        documents.modifyDocument(document);
    }
}
