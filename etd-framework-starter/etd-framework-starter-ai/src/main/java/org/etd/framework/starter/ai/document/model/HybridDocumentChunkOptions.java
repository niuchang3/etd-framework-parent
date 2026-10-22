package org.etd.framework.starter.ai.document.model;

import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.springframework.util.Assert;
/**
 * 结构与 Token 感知的混合分块；tokenizer 应与向量模型匹配，由解析服务加载。
 * @param useMarkdownTables 是否将表格序列化为 Markdown
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
        Boolean mergePeers) implements DocumentChunkOptions {

    /** 校验选项，不把显式空字符串当作未配置。 */
    public HybridDocumentChunkOptions {
        Assert.isTrue(maxTokens == null || maxTokens > 0, "分块Token上限必须大于零");
        if (tokenizer != null) {
            Assert.hasText(tokenizer, "tokenizer不能为空白");
        }
    }

    /** @return 混合分块策略 */
    @Override
    public DocumentChunkStrategy strategy() {
        return DocumentChunkStrategy.HYBRID;
    }
}
