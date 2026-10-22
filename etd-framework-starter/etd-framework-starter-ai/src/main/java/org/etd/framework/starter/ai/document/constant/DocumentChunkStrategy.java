package org.etd.framework.starter.ai.document.constant;

/** 原生文档分块策略；代码用于能力描述，不依赖枚举名称或序号。 */
public enum DocumentChunkStrategy {
    /** 根据文档层级及结构生成块。 */
    HIERARCHICAL("hierarchical"),
    /** 在结构分块基础上按 tokenizer 和 Token 上限进一步拆分、合并。 */
    HYBRID("hybrid");

    /** 稳定策略代码。 */
    private final String code;

    DocumentChunkStrategy(String code) {
        this.code = code;
    }

    /** @return 稳定策略代码 */
    public String getCode() {
        return code;
    }
}
