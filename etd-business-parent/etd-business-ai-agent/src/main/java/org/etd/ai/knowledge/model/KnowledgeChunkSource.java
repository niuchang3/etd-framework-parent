package org.etd.ai.knowledge.model;

import java.util.List;

/** 合并后仍保留全部来源引用，不虚构页内精确坐标。 */
public record KnowledgeChunkSource(
        /** 来源标题路径或合并后的标题集合。 */
        List<String> headings,
        /** 可选的图表说明，用于补充向量检索语义；解析器未提供时为空。 */
        List<String> captions,
        /** 来源页码，从1开始，无分页语义时为空。 */
        List<Integer> pageNumbers,
        /** Docling结构块引用。 */
        List<String> documentItemRefs) {
    /** 固定来源集合，防止保存后被调用方修改。 */
    public KnowledgeChunkSource {
        headings = List.copyOf(headings);
        captions = captions == null ? List.of() : List.copyOf(captions);
        pageNumbers = List.copyOf(pageNumbers);
        documentItemRefs = List.copyOf(documentItemRefs);
    }
}
