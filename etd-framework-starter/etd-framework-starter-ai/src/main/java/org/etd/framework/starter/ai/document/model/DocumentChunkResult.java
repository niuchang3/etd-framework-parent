package org.etd.framework.starter.ai.document.model;

import java.util.List;
/**
 * 原生分块结果，不把空片段列表或任务完成推断成完整解析成功。
 * @param chunks 引擎返回的片段，保留引擎原有顺序
 * @param documents 请求返回原文时的逐文档结果；未请求时为空列表
 * @param processingSeconds 引擎报告的处理时间，单位秒，可为空
 */
public record DocumentChunkResult(
        List<DocumentChunk> chunks,
        List<DocumentChunkDocument> documents,
        Double processingSeconds) {

    /** 固定结果容器，正文和错误详情不进入字符串表示。 */
    public DocumentChunkResult {
        chunks = List.copyOf(chunks);
        documents = List.copyOf(documents);
    }

    @Override
    public String toString() {
        return "DocumentChunkResult[chunks=" + chunks.size() + ", 内容已隐藏]";
    }
}
