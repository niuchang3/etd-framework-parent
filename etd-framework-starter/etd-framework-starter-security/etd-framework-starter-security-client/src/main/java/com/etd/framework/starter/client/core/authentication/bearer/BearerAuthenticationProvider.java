package com.etd.framework.starter.client.core.authentication.bearer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;
import com.etd.framework.starter.client.core.i18n.SecurityMessageCode;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.storage.UserLoginTokenStorage;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * Bearer 令牌认证提供者。
 * <p>
 * 负责校验 JWT 签名、令牌类型、过期时间以及 Redis 中保存的当前有效令牌。
 */
@Slf4j
public class BearerAuthenticationProvider implements AuthenticationProvider {


    private final TokenDecode<SignedJWT> tokenDecode;

    /**
     * 全局 JSON 映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 用户登录令牌存储。
     */
    private final UserLoginTokenStorage tokenStorage;


    /**
     * 创建 Bearer 认证提供者。
     *
     * @param tokenDecode 令牌解码器
     * @param objectMapper 全局 JSON 映射器
     * @param tokenStorage 用户登录令牌存储
     */
    public BearerAuthenticationProvider(TokenDecode<SignedJWT> tokenDecode,
                                        ObjectMapper objectMapper,
                                        UserLoginTokenStorage tokenStorage) {
        Assert.notNull(tokenDecode, "令牌解码器不能为空。");
        Assert.notNull(objectMapper, "JSON 映射器不能为空。");
        Assert.notNull(tokenStorage, "用户登录令牌存储不能为空。");
        this.tokenDecode = tokenDecode;
        this.objectMapper = objectMapper;
        this.tokenStorage = tokenStorage;
    }

    /**
     * 认证访问令牌，并把用户信息写入认证上下文。
     *
     * @param authentication Bearer 认证请求
     * @return 已认证的 Bearer 认证对象
     */
    /**
     * authenticate
     *
     * @param authentication 参数 authentication
     * @return 处理结果
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        BearerTokenAuthentication tokenAuthentication = (BearerTokenAuthentication) authentication;
        try {
            SignedJWT jwt = (SignedJWT) tokenDecode.decode(tokenAuthentication.getCredentials());
            verifyExpired(jwt);
            verifyAccessToken(jwt);

            UserDetails userDetails = toUserDetails(jwt);
            // 认证用户只归属一个租户，忽略客户端伪造的租户请求头。
            RequestContext.setTenantCode(userDetails.getTenantId());
            // Redis 中不存在或值不一致，都视为令牌已被后续登录或退出操作撤销。
            boolean existAccessToken = tokenStorage.isAccessTokenPresent(String.valueOf(userDetails.getId()));
            if (!existAccessToken) {
                throw new CredentialsExpiredException(SecurityMessageCode.TOKEN_REVOKED);
            }
            boolean accessMatches = tokenStorage.accessTokenMatches(String.valueOf(userDetails.getId()), tokenAuthentication.getCredentials());
            if (!accessMatches) {
                throw new CredentialsExpiredException(SecurityMessageCode.TOKEN_REVOKED);
            }


            BearerTokenAuthentication newAuthentication = new BearerTokenAuthentication(userDetails.getAuthorities());
            newAuthentication.setCredentials(tokenAuthentication.getCredentials());
            newAuthentication.setPrincipal(String.valueOf(userDetails.getId()));
            newAuthentication.setAuthenticated(true);
            newAuthentication.setDetails(userDetails);
            return newAuthentication;

        } catch (JOSEException | IllegalArgumentException | ParseException e) {
            // 不打印令牌原文，避免认证失败日志泄露敏感凭证。
            log.debug("令牌解析失败：{}", e.getMessage());
            throw new CredentialsExpiredException(SecurityMessageCode.TOKEN_PARSE_FAILED);
        }
    }

    /**
     * supports
     *
     * @param authentication 参数 authentication
     * @return 处理结果
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthentication.class.isAssignableFrom(authentication);
    }

    /**
     * 校验令牌是否过期。
     *
     * @param jwt 已解析的 JWT
     */
    private void verifyExpired(SignedJWT jwt) throws ParseException {
        Date expirationTime = jwt.getJWTClaimsSet().getExpirationTime();
        long now = Calendar.getInstance().getTime().getTime();
        long expired = expirationTime.getTime();
        if (now >= expired) {
            throw new CredentialsExpiredException(SecurityMessageCode.TOKEN_EXPIRED);
        }
    }

    /**
     * 只允许访问令牌访问受保护接口，刷新令牌不能直接作为 Bearer 凭证使用。
     *
     * @param jwt 已解析的 JWT
     */
    private void verifyAccessToken(SignedJWT jwt) {
        String tokenType = (String) jwt.getHeader().getCustomParam(SecurityParameterConstant.TokenType.class.getName());
        if (!SecurityParameterConstant.TokenType.ACCESS_TOKEN.getCode().equals(tokenType)) {
            throw new BadCredentialsException(SecurityMessageCode.TOKEN_TYPE_INVALID);
        }
    }

    /**
     * 从 JWT 载荷中还原登录用户信息。
     *
     * @param jwt 已解析的 JWT
     * @return 登录用户信息
     */
    private UserDetails toUserDetails(SignedJWT jwt) throws ParseException {
        Object user = jwt.getJWTClaimsSet().getClaim(Authentication.class.getName());
        if (user == null) {
            throw new BadCredentialsException(SecurityMessageCode.TOKEN_INVALID);
        }
        UserDetails userDetails = objectMapper.convertValue(user, UserDetails.class);
        if (userDetails == null || userDetails.getId() == null) {
            throw new BadCredentialsException(SecurityMessageCode.TOKEN_INVALID);
        }
        return userDetails;
    }
}
