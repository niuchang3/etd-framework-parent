package com.etd.framework.starter.oauth.authentication.oauth2.client;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 从平台结构化OAuth2表组装Spring授权服务器客户端。
 */
public class OAuth2RegisteredClientRepository implements RegisteredClientRepository {

    public static final String RESOURCE_CODE_SETTING = "etd.oauth2.resource-code";

    public static final String CLIENT_LEVEL_SETTING = "etd.oauth2.client-level";

    public static final String TRUSTED_SETTING = "etd.oauth2.trusted";

    private final OAuth2ClientRepository clientRepository;

    public OAuth2RegisteredClientRepository(OAuth2ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("OAuth2客户端请通过oauth2_client结构化表维护。");
    }

    /**
     * 根据Spring授权服务器内部客户端主键查询客户端。
     *
     * @param id 客户端主键
     * @return 注册客户端
     */
    @Override
    public RegisteredClient findById(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        try {
            // Spring授权服务器使用字符串ID，这里转回平台客户端主键查询。
            return clientRepository.findById(Long.valueOf(id))
                    .filter(this::isAvailable)
                    .map(this::toRegisteredClient)
                    .orElse(null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 根据OAuth2客户端ID查询客户端。
     *
     * @param clientId OAuth2客户端ID
     * @return 注册客户端
     */
    @Override
    public RegisteredClient findByClientId(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return null;
        }
        return clientRepository.findByClientId(clientId)
                .filter(this::isAvailable)
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    /**
     * 判断客户端是否可用于签发令牌。
     *
     * @param client 客户端
     * @return 是否可用
     */
    private boolean isAvailable(OAuth2Client client) {
        return Boolean.TRUE.equals(client.getEnabled()) && !Boolean.TRUE.equals(client.getLocked());
    }

    /**
     * 将平台客户端主数据转换成Spring授权服务器客户端模型。
     *
     * @param client 平台客户端主数据
     * @return 注册客户端
     */
    private RegisteredClient toRegisteredClient(OAuth2Client client) {
        RegisteredClient.Builder builder = RegisteredClient.withId(String.valueOf(client.getId()))
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .clientIdIssuedAt(client.getCreateTime() == null ? Instant.now() : client.getCreateTime())
                .clientSettings(buildClientSettings(client))
                .tokenSettings(buildTokenSettings(client));

        if (StringUtils.hasText(client.getClientSecret())) {
            // public客户端没有密钥，confidential客户端才需要设置密钥。
            builder.clientSecret(client.getClientSecret());
        }

        splitValues(client.getClientAuthenticationMethods()).forEach(method ->
                builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method)));
        splitValues(client.getAuthorizationGrantTypes()).forEach(grantType ->
                builder.authorizationGrantType(new AuthorizationGrantType(grantType)));
        splitValues(client.getRedirectUris()).forEach(builder::redirectUri);
        splitValues(client.getPostLogoutRedirectUris()).forEach(builder::postLogoutRedirectUri);
        getScopes(client).forEach(builder::scope);
        return builder.build();
    }

    /**
     * 构建OAuth2客户端行为配置。
     *
     * @param client 平台客户端主数据
     * @return 客户端配置
     */
    private ClientSettings buildClientSettings(OAuth2Client client) {
        return ClientSettings.builder()
                .requireProofKey(Boolean.TRUE.equals(client.getRequirePkce()))
                .requireAuthorizationConsent(Boolean.TRUE.equals(client.getRequireAuthorizationConsent()))
                .setting(RESOURCE_CODE_SETTING, client.getResourceCode())
                .setting(CLIENT_LEVEL_SETTING, client.getClientLevel())
                .setting(TRUSTED_SETTING, Boolean.TRUE.equals(client.getTrusted()))
                .build();
    }

    /**
     * 构建令牌生命周期配置。
     *
     * @param client 平台客户端主数据
     * @return 令牌配置
     */
    private TokenSettings buildTokenSettings(OAuth2Client client) {
        TokenSettings.Builder builder = TokenSettings.builder();
        if (client.getAccessTokenTtl() != null && client.getAccessTokenTtl() > 0) {
            builder.accessTokenTimeToLive(Duration.ofSeconds(client.getAccessTokenTtl()));
        }
        if (client.getRefreshTokenTtl() != null && client.getRefreshTokenTtl() > 0) {
            builder.refreshTokenTimeToLive(Duration.ofSeconds(client.getRefreshTokenTtl()));
        }
        return builder.build();
    }

    /**
     * 拆分逗号或空白分隔的配置值。
     *
     * @param value 配置值
     * @return 拆分后的集合
     */
    private Set<String> splitValues(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        return Arrays.stream(value.split("[,\\s]+"))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 获取客户端授权范围，引用层未返回scope时按空集合处理。
     *
     * @param client 平台客户端主数据
     * @return 授权范围集合
     */
    private Set<String> getScopes(OAuth2Client client) {
        if (client.getScopes() == null) {
            return Set.of();
        }
        return client.getScopes();
    }
}
