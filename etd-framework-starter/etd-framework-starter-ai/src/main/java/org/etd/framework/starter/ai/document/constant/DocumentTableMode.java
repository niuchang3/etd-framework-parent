package org.etd.framework.starter.ai.document.constant;

import com.fasterxml.jackson.annotation.JsonValue;

/** 表格结构识别精度，协议值不依赖枚举名称。 */
public enum DocumentTableMode {
    /** 优先处理速度。 */
    FAST("fast"),
    /** 优先识别准确性。 */
    ACCURATE("accurate");

    /** Docling使用的稳定协议代码。 */
    private final String code;

    DocumentTableMode(String code) {
        this.code = code;
    }

    /** @return 当前选项的协议代码 */
    @JsonValue
    public String getCode() {
        return code;
    }
}
