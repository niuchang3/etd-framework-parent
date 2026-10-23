package org.etd.framework.starter.ai.document.model;

import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.validation.DocumentOptionValidator;
/**
 * 结构与 Token 感知的混合分块；tokenizer 应与向量模型匹配，由解析服务加载。
 * @param useMarkdownTables 是否将表格序列化为 Markdown
 * @param useMarkdownImages 是否序列化图片引用；为空时使用服务默认值
 * @param imagePlaceholder 图片占位文本，允许空字符串；为空时使用服务默认值
 * @param includeRawText 是否同时返回原始正文
 * @param maxTokens 块的 Token 上限，必须大于零；为空时由 tokenizer 决定
 * @param tokenizer 解析服务可访问的 tokenizer 名称；为空时使用服务默认值
 * @param mergePeers 是否合并相同标题下相邻小块；为空时使用服务默认值
 */
public record HybridDocumentChunkOptions(
        boolean useMarkdownTables,
        boolean includeRawText,
        Integer maxTokens,
        String tokenizer,
        Boolean mergePeers,
        Boolean useMarkdownImages,
        String imagePlaceholder) implements DocumentChunkOptions {

    /** 校验选项，不把显式空字符串当作未配置。 */
    public HybridDocumentChunkOptions {
        DocumentOptionValidator.requireTokenCount(maxTokens, "分块Token上限");
        DocumentOptionValidator.requireTextWhenPresent(tokenizer, "分词器");
    }

    /** @return 混合分块策略 */
    @Override
    public DocumentChunkStrategy strategy() {
        return DocumentChunkStrategy.HYBRID;
    }
}
