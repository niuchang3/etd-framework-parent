package com.etd.framework.starter.oauth.authentication.oauth2.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * OAuth2授权中心网页登录态配置项。
 */
@Data
@ConfigurationProperties(prefix = "system.security.oauth2.session")
public class OAuth2SessionProperties {

    /**
     * 是否启用OAuth2网页登录态。
     */
    private Boolean enabled = false;

    /**
     * Redis中Session的空闲过期时间。
     */
    private Duration timeout = Duration.ofMinutes(30);

    /**
     * Redis中Session键的命名空间。
     */
    private String namespace = "etd:oauth2:session";

    /**
     * 浏览器保存Session ID的Cookie名称。
     */
    private String cookieName = "ETD_JSESSIONID";

    /**
     * 前端统一登录页地址。
     */
    private String loginPage = "/login";

    /**
     * 前端OAuth2授权确认页地址。
     */
    private String consentPage = "/consent";

    /**
     * 登录页和内部登录接口承载OAuth2回跳地址的参数名。
     */
    private String redirectParameter = "redirect";

    /**
     * 允许登录成功后回跳的OAuth2授权端点。
     */
    private String authorizeEndpoint = "/oauth2/authorize";

    /**
     * Session Cookie的SameSite策略。
     */
    private String sameSite = "Lax";
}
