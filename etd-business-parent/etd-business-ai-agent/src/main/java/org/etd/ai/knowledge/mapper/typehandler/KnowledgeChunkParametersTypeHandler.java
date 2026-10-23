package org.etd.ai.knowledge.mapper.typehandler;

import org.etd.framework.starter.ai.document.model.DocumentChunkParameters;

/** 三级分片参数的JSONB持久化。 */
public final class KnowledgeChunkParametersTypeHandler extends KnowledgeJsonTypeHandler<DocumentChunkParameters> {
    /** 显式绑定反序列化目标，保留嵌套字段和枚举类型。 */
    public KnowledgeChunkParametersTypeHandler() { super(DocumentChunkParameters.class); }
}
