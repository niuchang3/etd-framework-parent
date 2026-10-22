package org.etd.framework.starter.ai.document.model;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import org.etd.framework.starter.ai.document.constant.DocumentParseStatus;
/**
 * 单份文档的解析产物；包含敏感正文，不应直接记录日志或返回前端。
 * @param status 文档处理结果，部分成功必须由调用方显式处理
 * @param markdown Markdown 正文，可为空
 * @param blocks 按正文树阅读顺序排列的内容块
 * @param structuredDocument 引擎原始结构化文档，仅供诊断或专属扩展，不用于公共业务契约
 * @param processingSeconds 引擎报告的处理时长，单位秒，可为空
 * @param warnings 引擎报告的信息，可能包含文档内容，禁止直接记录日志
 */
public record DocumentParseResult(
        DocumentParseStatus status,
        String markdown,
        List<DocumentBlock> blocks,
        JsonNode structuredDocument,
        Double processingSeconds,
        List<String> warnings) {

    /** 固定集合快照；原始 JSON 属于本次结果，调用方不得跨请求共享修改。 */
    public DocumentParseResult {
        blocks = List.copyOf(blocks);
        warnings = List.copyOf(warnings);
    }

    @Override
    public String toString() {
        return "DocumentParseResult[status=" + status + ", 内容已隐藏]";
    }
}
