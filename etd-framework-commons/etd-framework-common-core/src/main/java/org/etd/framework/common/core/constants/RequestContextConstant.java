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
    REQUEST_IP("REQUEST-IP", "请求的IP地址"),
    TENANT_CODE("TENANT-CODE", "租户CODE"),
    PRODUCT_CODE("PRODUCT-CODE", "产品CODE"),
    TOKEN("TOKEN", "系统派发的token令牌"),
    USER_CODE("USER-CODE", "用户Code"),
    USER_NAME("USER-NAME", "用户名称"),
    USER_ROLE("USER-ROLE", "用户角色");

    @Getter
    private String code;
    @Getter
    private String explanation;

    RequestContextConstant(String code, String explanation) {
        this.code = code;
        this.explanation = explanation;
    }
}
