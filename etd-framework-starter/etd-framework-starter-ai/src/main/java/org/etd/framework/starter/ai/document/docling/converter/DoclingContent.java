package org.etd.framework.starter.ai.document.docling.converter;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import org.etd.framework.starter.ai.document.model.DocumentBlock;

/**
 * 适配器内部结构提取快照。
 * @param document 引擎原始结构
 * @param blocks 按正文树阅读顺序排列的内容块
 */
public record DoclingContent(JsonNode document, List<DocumentBlock> blocks) {
}
