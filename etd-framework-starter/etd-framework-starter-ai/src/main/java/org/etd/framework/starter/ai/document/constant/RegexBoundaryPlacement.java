package org.etd.framework.starter.ai.document.constant;

import com.fasterxml.jackson.annotation.JsonValue;

/** 正则匹配内容的保留位置；协议值显式定义，不依赖枚举名称或序号。 */
public enum RegexBoundaryPlacement {
    /** 匹配内容归入下一分片，适合保留章节标题。 */
    NEXT("next"),
    /** 匹配内容归入上一分片。 */
    PREVIOUS("previous"),
    /** 丢弃作为边界的匹配内容。 */
    DISCARD("discard");

    /** 对外使用的稳定协议代码。 */
    private final String code;

    RegexBoundaryPlacement(String code) {
        this.code = code;
    }

    /** @return 匹配边界的保留方式 */
    @JsonValue
    public String getCode() {
        return code;
    }
}
