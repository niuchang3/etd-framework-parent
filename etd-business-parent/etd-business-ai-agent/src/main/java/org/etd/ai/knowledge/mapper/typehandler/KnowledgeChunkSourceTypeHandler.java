package org.etd.ai.knowledge.mapper.typehandler;

import org.etd.ai.knowledge.model.KnowledgeChunkSource;

/** 分片来源的JSONB持久化。 */
public final class KnowledgeChunkSourceTypeHandler extends KnowledgeJsonTypeHandler<KnowledgeChunkSource> {
    /** 显式绑定反序列化目标，保留嵌套字段和枚举类型。 */
    public KnowledgeChunkSourceTypeHandler() { super(KnowledgeChunkSource.class); }
}
