package org.etd.ai.knowledge.biz;

import org.etd.framework.common.core.constants.BasicConstant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgeChunkLimits;
import org.etd.ai.knowledge.constant.KnowledgeChunkStatus;
import org.etd.ai.knowledge.constant.KnowledgeVectorStatus;
import org.etd.ai.knowledge.model.KnowledgeChunkSource;
import org.etd.ai.knowledge.converter.KnowledgeChunkConverter;
import org.etd.ai.knowledge.service.KnowledgeChunkService;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.document.tokenizer.DocumentTokenCounter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import org.etd.ai.knowledge.entity.KnowledgeChunkEntity;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;

/** 分片人工编辑、相邻合并、拆分、删除和确认；以资料行锁和版本串行化整份结果的修改。 */
@Service
@RequiredArgsConstructor
public class KnowledgeChunkEditBizService {
    private final KnowledgeDocumentService documents;
    private final KnowledgeChunkService chunks;
    private final KnowledgeChunkConfirmBizService confirmation;
    /** @param id 资料ID @param version 资料版本 @param edits 分片ID与新正文 @return 最新资料状态 */
    @Transactional
    public KnowledgeDocumentEntity modifyChunks(Long id, Long version, Map<Long, String> edits) {
        KnowledgeDocumentEntity document = requireEditable(id, version);
        for (Map.Entry<Long, String> entry : edits.entrySet()) {
            KnowledgeChunkEntity chunk = chunks.requireChunk(id, entry.getKey());
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
        KnowledgeDocumentEntity document = requireEditable(id, version);
        List<KnowledgeChunkEntity> records = chunks.fetchChunkList(id);
        List<KnowledgeChunkEntity> selected = requireAdjacent(records, ids);
        String content = mergeContent(selected);
        KnowledgeChunkSource source = mergeSource(selected);
        KnowledgeChunkEntity first = selected.getFirst();
        first.setContent(content);
        first.setTokenCount(DocumentTokenCounter.count(content));
        first.setSource(source);
        chunks.modifyChunk(first);
        // 合并结果保留第一片ID，其余被合并分片在同一事务内删除。
        for (int index = 1; index < selected.size(); index++) {
            chunks.removeChunk(selected.get(index).getId());
        }
        document.setChunkCount(records.size() - selected.size() + 1);
        markDraft(document);
        return document;
    }

    /**
     * 将一片拆成两片，使用用户提交的最终正文，不重新调用Docling。
     * @param id 资料ID
     * @param chunkId 原分片ID，第一片保留此ID
     * @param version 当前资料版本
     * @param firstContent 第一片正文
     * @param secondContent 第二片正文，两片均继承原来源，不能据此推断精确页码
     * @return 数量加一、版本递增且确认状态已清除的资料
     * @throws ApiRuntimeException 权限、状态、版本、正文或数量不符合要求时
     */
    @Transactional
    public KnowledgeDocumentEntity splitChunk(Long id, Long chunkId, Long version,
            String firstContent, String secondContent) {
        KnowledgeDocumentEntity document = requireEditable(id, version);
        requireSplitContent(firstContent);
        requireSplitContent(secondContent);
        if (document.getChunkCount() >= KnowledgeChunkLimits.MAX_CHUNKS) {
            throw new ApiRuntimeException("分片数量已达上限，不能继续拆分");
        }
        KnowledgeChunkEntity original = chunks.requireChunk(id, chunkId);
        KnowledgeChunkEntity second = KnowledgeChunkConverter.INSTANCE.toSplitEntity(original, secondContent);
        second.setChunkIndex(Math.incrementExact(original.getChunkIndex()));
        chunks.moveFollowingChunkList(id, original.getChunkIndex());
        original.setContent(firstContent);
        original.setTokenCount(DocumentTokenCounter.count(firstContent));
        chunks.modifyChunk(original);
        List<KnowledgeChunkEntity> newChunks = List.of(second);
        chunks.createChunkList(newChunks);
        document.setChunkCount(document.getChunkCount() + 1);
        markDraft(document);
        return document;
    }

    /** 在同一业务入口约束两段正文，内部调用也不能绕过HTTP校验写入无效内容。 */
    private void requireSplitContent(String content) {
        if (content == null || content.isBlank() || content.length() > KnowledgeChunkLimits.MAX_CONTENT_CHARACTERS) {
            throw new ApiRuntimeException("拆分后的每片正文不能为空白且不能超过长度限制");
        }
    }

    /**
     * 删除错误分片；允许删除最后一片，空结果不能确认，重新生成会重新计算全部分片。
     * @param id 资料ID
     * @param chunkId 必须属于当前资料的分片ID
     * @param version 当前资料版本
     * @return 数量和版本已更新的待确认状态
     * @throws ApiRuntimeException 无维护权限、生成中、版本冲突或分片不存在时
     */
    @Transactional
    public KnowledgeDocumentEntity removeChunk(Long id, Long chunkId, Long version) {
        KnowledgeDocumentEntity document = requireEditable(id, version);
        KnowledgeChunkEntity chunk = chunks.requireChunk(id, chunkId);
        chunks.removeChunk(chunk.getId());
        document.setChunkCount(document.getChunkCount() - 1);
        markDraft(document);
        return document;
    }

    /** @param id 资料ID @param version 当前版本 @return 当前检索模式索引完成后的确认状态 */
    public KnowledgeDocumentEntity confirmChunks(Long id, Long version) {
        return confirmation.confirmChunks(id, version);
    }

    /** 编辑前持有资料行锁，先校验生成状态、版本和现有分片数量。 */
    private KnowledgeDocumentEntity requireEditable(Long id, Long version) {
        KnowledgeDocumentEntity document = documents.requireDocument(id, true);
        documents.requireChunkIdle(document);
        if (!Objects.equals(document.getVersion(), version)) {
            throw new ApiRuntimeException("分片已变化，请刷新后重试");
        }
        if (document.getChunkCount() == null || document.getChunkCount() == 0) {
            throw new ApiRuntimeException("请先生成分片");
        }
        return document;
    }

    /** 按去重、定位首片、逐项核对原文顺序校验，禁止跨资料或跳片合并。 */
    private List<KnowledgeChunkEntity> requireAdjacent(List<KnowledgeChunkEntity> documentChunks, List<Long> selectedIds) {
        Set<Long> uniqueIds = new HashSet<>(selectedIds);
        if (selectedIds.size() < 2 || uniqueIds.size() != selectedIds.size()) {
            throw new ApiRuntimeException("至少选择两个不同的相邻分片");
        }
        int firstChunkIndex = findChunkIndex(documentChunks, selectedIds.getFirst());
        if (firstChunkIndex < 0 || firstChunkIndex + selectedIds.size() > documentChunks.size()) {
            throw new ApiRuntimeException("分片不属于当前资料");
        }
        List<KnowledgeChunkEntity> selectedChunks = new ArrayList<>();
        for (int offset = 0; offset < selectedIds.size(); offset++) {
            KnowledgeChunkEntity chunk = documentChunks.get(firstChunkIndex + offset);
            if (!chunk.getId().equals(selectedIds.get(offset))) {
                throw new ApiRuntimeException("只能按原文顺序合并相邻分片");
            }
            selectedChunks.add(chunk);
        }
        return selectedChunks;
    }

    /** 在当前资料的有序分片中定位首片，不存在时返回-1供上层统一拒绝。 */
    private int findChunkIndex(List<KnowledgeChunkEntity> documentChunks, Long chunkId) {
        for (int index = 0; index < documentChunks.size(); index++) {
            if (documentChunks.get(index).getId().equals(chunkId)) {
                return index;
            }
        }
        return -1;
    }

    /** 按原文顺序连接正文，并校验合并后的总长度。 */
    private String mergeContent(List<KnowledgeChunkEntity> selected) {
        StringBuilder content = new StringBuilder();
        for (KnowledgeChunkEntity chunk : selected) {
            if (!content.isEmpty()) {
                content.append(KnowledgeChunkLimits.CONTENT_SEPARATOR);
            }
            content.append(chunk.getContent());
        }
        if (content.length() > KnowledgeChunkLimits.MAX_CONTENT_CHARACTERS) {
            throw new ApiRuntimeException("合并后正文超过长度限制");
        }
        return content.toString();
    }

    /** 标题和引用按首次出现顺序去重，页码按数值升序合并。 */
    private KnowledgeChunkSource mergeSource(List<KnowledgeChunkEntity> selected) {
        Set<String> headings = new LinkedHashSet<>();
        Set<Integer> pages = new TreeSet<>();
        Set<String> refs = new LinkedHashSet<>();
        for (KnowledgeChunkEntity chunk : selected) {
            headings.addAll(chunk.getSource().headings());
            pages.addAll(chunk.getSource().pageNumbers());
            refs.addAll(chunk.getSource().documentItemRefs());
        }
        List<String> path = List.copyOf(headings);
        List<Integer> pageNumbers = List.copyOf(pages);
        List<String> references = List.copyOf(refs);
        return new KnowledgeChunkSource(path, pageNumbers, references);
    }

    /** 编辑后递增正文轮次、撤销确认并停用正式检索，旧向量状态按原规则失效。 */
    private void markDraft(KnowledgeDocumentEntity document) {
        document.setVectorStatus(KnowledgeVectorStatus.afterContentChange(document.getVectorStatus()));
        document.setDataStatus(BasicConstant.DataStatus.DISABLED_CODE);
        document.setProcessingVersion(Math.incrementExact(document.getProcessingVersion()));
        document.setChunkStatus(KnowledgeChunkStatus.DRAFT);
        document.setChunkConfirmedTime(null);
        documents.modifyDocument(document);
    }
}
