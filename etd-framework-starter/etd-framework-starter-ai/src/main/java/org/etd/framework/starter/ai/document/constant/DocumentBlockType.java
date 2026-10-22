package org.etd.framework.starter.ai.document.constant;

/** 解析后统一内容块类型。 */
public enum DocumentBlockType {
    /** 段落正文。 */
    TEXT("text"),
    /** 标题。 */
    HEADING("heading"),
    /** 列表项。 */
    LIST_ITEM("list_item"),
    /** 表格。 */
    TABLE("table"),
    /** 图片。 */
    PICTURE("picture"),
    /** 代码。 */
    CODE("code"),
    /** 公式。 */
    FORMULA("formula"),
    /** 其他内容。 */
    OTHER("other");

    /** 稳定协议代码，不依赖枚举序号或名称。 */
    private final String code;

    DocumentBlockType(String code) {
        this.code = code;
    }

    /** @return 稳定协议代码 */
    public String getCode() {
        return code;
    }
}
