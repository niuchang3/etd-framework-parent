package com.etd.framework.starter.oauth.authentication.oauth2.client;

import lombok.Data;

import java.time.Instant;
import java.util.Set;

/**
 * OAuth2客户端主数据。
 */
@Data
public class OAuth2Client {

    /**
     * 主键ID。
     */
    private Long id;

    /**
     * 资源编码，引用层可映射为应用编码。
     */
    private String resourceCode;

    /**
     * OAuth2客户端ID。
     */
    private String clientId;

    /**
     * 客户端显示名称。
     */
    private String clientName;

    /**
     * 客户端密钥，public客户端可以为空。
     */
    private String clientSecret;

    /**
     * 客户端类型，例如public或confidential。
     */
    private String clientType;

    /**
     * 客户端级别，例如official、third_party或internal_service。
     */
    private String clientLevel;

    /**
     * 客户端认证方式。
     */
    private String clientAuthenticationMethods;

    /**
     * 授权模式。
     */
    private String authorizationGrantTypes;

    /**
     * 授权回调地址集合。
     */
    private String redirectUris;

    /**
     * 退出登录回调地址集合。
     */
    private String postLogoutRedirectUris;

    /**
     * 是否可信客户端。
     */
    private Boolean trusted;

    /**
     * 是否要求PKCE。
     */
    private Boolean requirePkce;

    /**
     * 是否需要用户授权确认。
     */
    private Boolean requireAuthorizationConsent;

    /**
     * 访问令牌有效期，单位秒。
     */
    private Integer accessTokenTtl;

    /**
     * 刷新令牌有效期，单位秒。
     */
    private Integer refreshTokenTtl;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 是否锁定。
     */
    private Boolean locked;

    /**
     * 是否系统内置客户端。
     */
    private Boolean builtIn;

    /**
     * 创建时间。
     */
    private Instant createTime;

    /**
     * 数据状态。
     */
    private Integer dataStatus;

    /**
     * 客户端允许申请的权限范围。
     */
    private Set<String> scopes;
}
