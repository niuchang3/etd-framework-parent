package org.etd.ai.knowledge.model;

/** 一份资料删除失败的业务结果，不包含原件路径、服务端堆栈或存储错误正文。 */
public record KnowledgeDocumentDeleteFailure(
        /** 删除失败的资料ID。 */
        Long id,
        /** 可向用户展示的失败原因。 */
        String message) {
}
