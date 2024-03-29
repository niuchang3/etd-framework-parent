package org.etd.framework.common.core.constants;

import lombok.Getter;


/**
 * 请求上下文常量枚举类
 *
 * @author Administrator
 */
public enum RequestContextConstant {
    /**
     * 请求上下文信息所携带的基础信息
     */
    TRACE_ID("TRACE-ID", "链路ID"),
    TENANT_CODE("TENANT-CODE", "租户CODE"),
    TOKEN("Authorization", "系统派发的token令牌");

    @Getter
    private String code;
    @Getter
    private String explanation;

    RequestContextConstant(String code, String explanation) {
        this.code = code;
        this.explanation = explanation;
    }
}
