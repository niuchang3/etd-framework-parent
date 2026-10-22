package org.etd.framework.starter.ai.document.model;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
/**
 * 适合向量化的片段，text 保留引擎提供的上下文，rawText 是不含附加上下文的正文。
 * @param filename 源文件名
 * @param index 当前文档中的块序号，从零开始
 * @param text 用于检索或向量化的上下文文本
 * @param rawText 原始正文，未请求或服务未提供时为空
 * @param tokenCount 服务报告的 Token 数；未计算时为空，不是字符数
 * @param headings 片段所属的标题路径
 * @param captions 关联的图片或表格说明
 * @param documentItemRefs 源结构化文档内的内容块引用
 * @param pageNumbers 来源页码，从 1 开始；无分页语义时为空列表
 * @param metadata 引擎额外元数据，仅供受控扩展，不记录日志
 */
public record DocumentChunk(
        String filename,
        int index,
        String text,
        String rawText,
        Integer tokenCount,
        List<String> headings,
        List<String> captions,
        List<String> documentItemRefs,
        List<Integer> pageNumbers,
        Map<String, Object> metadata) {

    /** 固定容器快照，元数据的嵌套对象仍属于本次结果，不跨请求共享修改。 */
    public DocumentChunk {
        headings = List.copyOf(headings);
        captions = List.copyOf(captions);
        documentItemRefs = List.copyOf(documentItemRefs);
        pageNumbers = List.copyOf(pageNumbers);
        var metadataCopy = new LinkedHashMap<>(metadata);
        metadata = Collections.unmodifiableMap(metadataCopy);
    }

    @Override
    public String toString() {
        return "DocumentChunk[index=" + index + ", 内容已隐藏]";
    }
}
