package com.etd.framework.starter.client.core.constant;

/**
 * 内部登录认证参数常量。
 */
public interface SecurityParameterConstant {


    /**
     * Redis 中保存登录令牌的缓存前缀。
     */
    String TOKEN_CACHE = "TOKEN";

    String ACCESS_TOKEN_PARAMETER = "access_token";

    String REFRESH_TOKEN_PARAMETER = "refresh_token";

    /**
     * 框架内部支持的令牌类型。
     */
    enum TokenType {
        ACCESS_TOKEN(ACCESS_TOKEN_PARAMETER),
        REFRESH_TOKEN(REFRESH_TOKEN_PARAMETER);

        private final String code;

        TokenType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * JWT 用户信息 Claim 字段。
     */
    interface UserClaim {
        String ID = "id";
        String ACCOUNT = "account";
        String MOBILE = "mobile";
        String USER_NAME = "userName";
        String BIRTHDAY = "birthday";
        String GENDER = "gender";
        String AVATAR = "avatar";
        String NICK_NAME = "nickName";
        String LOCKED = "locked";
        String ENABLED = "enabled";
        String TENANT_ID = "tenantId";
        String ROLE_CODES = "roleCodes";
        String PLATFORM_ADMIN = "platformAdmin";
        String TENANT_ADMIN = "tenantAdmin";
        String AUTHORITIES = "authorities";
    }

    /**
     * HTTP 认证方案。
     */
    enum TokenPrompt {
        Bearer
    }
}
