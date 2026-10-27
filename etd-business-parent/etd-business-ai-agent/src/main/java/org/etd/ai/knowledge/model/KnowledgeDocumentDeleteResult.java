package org.etd.ai.knowledge.model;

import java.util.List;

/** 批量资料删除结果，逐项提交，失败不会撤销其他资料已经完成的删除。 */
public record KnowledgeDocumentDeleteResult(
        /** 已成功删除的资料ID，保持请求中的相对顺序。 */
        List<Long> successIds,
        /** 失败资料及原因，保持请求中的相对顺序。 */
        List<KnowledgeDocumentDeleteFailure> failures) {
}
