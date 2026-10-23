package org.etd.framework.starter.ai.document.model;

import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
/**
 * 层级分块选项，不提供无实际支持的固定 Token 上限或重叠长度。
 * @param useMarkdownTables 是否将表格序列化为 Markdown
 * @param useMarkdownImages 是否序列化图片引用；为空时使用服务默认值
 * @param imagePlaceholder 图片占位文本，允许空字符串；为空时使用服务默认值
 * @param includeRawText 是否同时返回原始正文
 */
public record HierarchicalDocumentChunkOptions(
        boolean useMarkdownTables,
        boolean includeRawText,
        Boolean useMarkdownImages,
        String imagePlaceholder) implements DocumentChunkOptions {

    /** @return 层级分块策略 */
    @Override
    public DocumentChunkStrategy strategy() {
        return DocumentChunkStrategy.HIERARCHICAL;
    }
}
