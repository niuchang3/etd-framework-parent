package org.etd.framework.starter.ai.document.model;

import java.util.List;
import org.etd.framework.starter.ai.document.constant.DocumentBlockType;
/**
 * 文档中的单个内容块，列表次序即阅读顺序，保留父节点关系。
 * @param id 本次文档内的内容块标识
 * @param parentId 父内容块或分组标识，可为空
 * @param type 统一内容类型
 * @param text 提取文本；图片或无文本内容可以为空
 * @param headingLevel 标题层级；非标题或未知时为空
 * @param locations 原文位置；无分页语义或引擎未提供时为空列表
 * @param tableData 表格单元格结构；非表格时为空
 */
public record DocumentBlock(
        String id,
        String parentId,
        DocumentBlockType type,
        String text,
        Integer headingLevel,
        List<DocumentSourceLocation> locations,
        DocumentTable tableData) {

    /** 固定位置集合，正文和表格不进入自动生成的字符串表示。 */
    public DocumentBlock {
        locations = List.copyOf(locations);
    }

    @Override
    public String toString() {
        return "DocumentBlock[type=" + type + ", 内容已隐藏]";
    }
}
