package org.etd.framework.starter.ai.document.model;

import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;

/** 类型化的原生分块选项；不向业务泄露引擎专属 SDK 对象。 */
public sealed interface DocumentChunkOptions permits HierarchicalDocumentChunkOptions, HybridDocumentChunkOptions {
    /** @return 分块策略，必须包含在引擎声明的支持集合中 */
    DocumentChunkStrategy strategy();

    /** @return 是否将表格序列化为 Markdown，而不是三元组形式 */
    boolean useMarkdownTables();

    /** @return 是否在结果中同时保留不含标题等上下文的原始正文 */
    boolean includeRawText();
}
