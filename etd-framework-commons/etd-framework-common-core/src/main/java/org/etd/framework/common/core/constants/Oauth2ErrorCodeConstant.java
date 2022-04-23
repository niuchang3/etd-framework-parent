package org.etd.framework.common.core.constants;

public enum Oauth2ErrorCodeConstant implements RequestCodeConverter {
    /**
     * 权限类的操作从4000开始
     * 业务状态码：400 ，[认证权限等操作]，如何权限相关的操作码从4000开始向上定义
     */

    INVALID_REQUEST(4000, "非法的请求,invalid_request"),

    UNAUTHORIZED_CLIENT(4001, "未经授权的客户,unauthorized_client"),

    ACCESS_DENIED(4002, "拒绝访问,access_denied"),

    UNSUPPORTED_RESPONSE_TYPE(4003, "不支持响应类型,unsupported_response_type"),

    INVALID_SCOPE(4004, "无效的范围,invalid_scope"),

    INSUFFICIENT_SCOPE(4005, "范围不足,insufficient_scope"),

    INVALID_TOKEN(4006, "无效的令牌,invalid_token"),

    INVALID_CLIENT(4007, "无效的客户端,invalid_client"),

    INVALID_GRANT(4008, "无效的授权,invalid_grant"),

    UNSUPPORTED_GRANT_TYPE(4009, "不支持的授权类型,unsupported_grant_type"),

    UNSUPPORTED_TOKEN_TYPE(4010, "不支持的令牌类型,unsupported_token_type"),

    INVALID_REDIRECT_URI(4011, "无效的重定向地址,invalid_redirect_uri");

    Oauth2ErrorCodeConstant(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    private Integer code;

    private String description;

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getDescription() {
        return this.description;
    }

}
