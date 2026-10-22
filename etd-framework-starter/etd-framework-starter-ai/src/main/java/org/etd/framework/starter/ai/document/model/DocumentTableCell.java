package org.etd.framework.starter.ai.document.model;


/**
 * 统一表格单元格；位置未知时保留空值，不猜测合并关系。
 * @param text 单元格正文
 * @param firstRow 起始行，零基且包含该行
 * @param endRow 结束行，零基且不包含该行
 * @param firstColumn 起始列，零基且包含该列
 * @param endColumn 结束列，零基且不包含该列
 * @param columnHeader 是否为列标题
 * @param rowHeader 是否为行标题
 */
public record DocumentTableCell(
        String text,
        Integer firstRow,
        Integer endRow,
        Integer firstColumn,
        Integer endColumn,
        boolean columnHeader,
        boolean rowHeader) {

    @Override
    public String toString() {
        return "DocumentTableCell[内容已隐藏]";
    }
}
