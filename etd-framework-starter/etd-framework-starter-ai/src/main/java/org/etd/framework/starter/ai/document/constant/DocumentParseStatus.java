package org.etd.framework.starter.ai.document.constant;

/** 文档级解析结果，调用方必须处理部分成功。 */
public enum DocumentParseStatus {
    /** 完整成功。 */
    SUCCESS("success"),
    /** 部分成功。 */
    PARTIAL_SUCCESS("partial_success"),
    /** 解析失败。 */
    FAILURE("failure"),
    /** 文档被跳过。 */
    SKIPPED("skipped");

    /** 稳定协议代码，不依赖枚举序号或名称。 */
    private final String code;

    DocumentParseStatus(String code) {
        this.code = code;
    }

    /** @return 稳定协议代码 */
    public String getCode() {
        return code;
    }
}
