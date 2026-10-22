package org.etd.framework.starter.ai.document.model;

import org.springframework.util.Assert;
/**
 * 解析并分块的单文档请求；同步与异步共享同一选项。
 * @param document 已授权的文件及解析选项
 * @param options 原生分块策略及参数
 * @param includeConvertedDocument 是否同时返回原文解析结果；关闭时无法从本响应得知逐文档解析状态
 */
public record DocumentChunkRequest(
        DocumentParseRequest document,
        DocumentChunkOptions options,
        boolean includeConvertedDocument) {

    /** 校验请求，原文及配置不进入字符串表示。 */
    public DocumentChunkRequest {
        Assert.notNull(document, "分块文档不能为空");
        Assert.notNull(options, "分块选项不能为空");
    }

    @Override
    public String toString() {
        return "DocumentChunkRequest[文档内容已隐藏]";
    }
}
