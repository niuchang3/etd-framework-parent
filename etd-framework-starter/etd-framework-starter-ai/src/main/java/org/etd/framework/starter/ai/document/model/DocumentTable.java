package org.etd.framework.starter.ai.document.model;

import java.util.List;
/**
 * 统一表格结构；合并单元格用起止行列表示，不要求业务理解引擎字段名。
 * @param rowCount 行数；引擎未提供时为空
 * @param columnCount 列数；引擎未提供时为空
 * @param cells 单元格列表，按引擎给定顺序保留
 */
public record DocumentTable(
        Integer rowCount,
        Integer columnCount,
        List<DocumentTableCell> cells) {

    /** 固定单元格集合，表格正文不进入自动生成的字符串表示。 */
    public DocumentTable {
        cells = List.copyOf(cells);
    }

    @Override
    public String toString() {
        return "DocumentTable[内容已隐藏]";
    }
}
