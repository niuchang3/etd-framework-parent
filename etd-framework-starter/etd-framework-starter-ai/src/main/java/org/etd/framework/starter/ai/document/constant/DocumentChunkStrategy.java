package org.etd.framework.starter.ai.document.constant;

import com.fasterxml.jackson.annotation.JsonValue;

/** 文档分片策略；正则策略用于Docling解析后的二次处理，不属于原生切分能力。 */
public enum DocumentChunkStrategy {
    /** 根据文档层级及结构生成块。 */
    HIERARCHICAL("hierarchical"),
    /** 在结构分块基础上按 tokenizer 和 Token 上限进一步拆分、合并。 */
    HYBRID("hybrid"),
    /** 在Docling解析后按正则规则二次切分；由Docling解析后独立执行。 */
    REGEX("regex");

    /** 稳定策略代码。 */
    private final String code;

    DocumentChunkStrategy(String code) {
        this.code = code;
    }

    /** @return 稳定策略代码 */
    @JsonValue
    public String getCode() {
        return code;
    }
}
