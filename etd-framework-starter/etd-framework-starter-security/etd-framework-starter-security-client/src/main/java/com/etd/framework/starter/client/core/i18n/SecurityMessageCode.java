package com.etd.framework.starter.client.core.i18n;

/**
 * Security 模块国际化异常 Message Code 常量定义。
 */
public interface SecurityMessageCode {

    /**
     * 未认证或凭证无效
     */
    String UNAUTHORIZED = "security.unauthorized";

    /**
     * 权限不足
     */
    String ACCESS_DENIED = "security.access-denied";

    /**
     * 令牌不能为空
     */
    String TOKEN_EMPTY = "security.token.empty";

    /**
     * 令牌无效
     */
    String TOKEN_INVALID = "security.token.invalid";

    /**
     * 令牌解析失败
     */
    String TOKEN_PARSE_FAILED = "security.token.parse-failed";

    /**
     * 令牌已过期
     */
    String TOKEN_EXPIRED = "security.token.expired";

    /**
     * 令牌已被撤销
     */
    String TOKEN_REVOKED = "security.token.revoked";

    /**
     * 令牌类型错误
     */
    String TOKEN_TYPE_INVALID = "security.token.type-invalid";

    /**
     * 账号已被禁用
     */
    String ACCOUNT_DISABLED = "security.account.disabled";

    /**
     * 用户不存在
     */
    String USER_NOT_FOUND = "security.user.not-found";

    /**
     * 密码错误
     */
    String PASSWORD_INVALID = "security.password.invalid";

    /**
     * 凭证错误
     */
    String CREDENTIALS_INVALID = "security.credentials.invalid";

    /**
     * 请求体类型错误
     */
    String REQUEST_CONTENT_TYPE_INVALID = "security.request.content-type-invalid";

    /**
     * 请求体解析失败
     */
    String REQUEST_BODY_PARSE_FAILED = "security.request.body-parse-failed";
}
