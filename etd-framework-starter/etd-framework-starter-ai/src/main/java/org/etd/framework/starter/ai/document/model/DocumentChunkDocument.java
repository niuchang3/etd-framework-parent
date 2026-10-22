package org.etd.framework.starter.ai.document.model;


/**
 * 分块请求附带的原文解析结果；状态与分块任务执行状态独立。
 * @param filename 源文件名，失败且引擎未提供时可为空
 * @param result 文档级状态、诊断信息及原文内容
 */
public record DocumentChunkDocument(
        String filename,
        DocumentParseResult result) {

    @Override
    public String toString() {
        return "DocumentChunkDocument[内容已隐藏]";
    }
}
