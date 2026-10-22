package org.etd.framework.starter.ai.document.model;

import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
/**
 * 层级分块选项，不提供无实际支持的固定 Token 上限或重叠长度。
 * @param useMarkdownTables 是否将表格序列化为 Markdown
 * @param includeRawText 是否同时返回原始正文
 */
public record HierarchicalDocumentChunkOptions(
        boolean useMarkdownTables,
        boolean includeRawText) implements DocumentChunkOptions {

    /** @return 层级分块策略 */
    @Override
    public DocumentChunkStrategy strategy() {
        return DocumentChunkStrategy.HIERARCHICAL;
    }
}
