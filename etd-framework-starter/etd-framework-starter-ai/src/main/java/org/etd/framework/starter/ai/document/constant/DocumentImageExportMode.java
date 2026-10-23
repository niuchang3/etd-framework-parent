package org.etd.framework.starter.ai.document.constant;

import com.fasterxml.jackson.annotation.JsonValue;

/** 原文图片导出方式，协议值不依赖枚举名称。 */
public enum DocumentImageExportMode {
    /** 仅输出占位符。 */
    PLACEHOLDER("placeholder"),
    /** 图片内嵌到原文结果。 */
    EMBEDDED("embedded"),
    /** 引用图片资源。 */
    REFERENCED("referenced");

    /** Docling使用的稳定协议代码。 */
    private final String code;

    DocumentImageExportMode(String code) {
        this.code = code;
    }

    /** @return 当前选项的协议代码 */
    @JsonValue
    public String getCode() {
        return code;
    }
}
