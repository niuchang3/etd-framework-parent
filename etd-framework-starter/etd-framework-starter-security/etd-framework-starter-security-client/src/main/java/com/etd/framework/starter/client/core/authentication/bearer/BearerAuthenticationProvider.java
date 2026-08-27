package com.etd.framework.starter.client.core.authentication.bearer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.storage.TokenStorage;
import com.etd.framework.starter.client.core.user.UserDetails;
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
     * 创建 Bearer 认证提供者。
     *
     * @param tokenDecode 令牌解码器
     * @param objectMapper 全局 JSON 映射器
     */
    public BearerAuthenticationProvider(TokenDecode<SignedJWT> tokenDecode, ObjectMapper objectMapper) {
        Assert.notNull(tokenDecode, "令牌解码器不能为空。");
        Assert.notNull(objectMapper, "JSON 映射器不能为空。");
        this.tokenDecode = tokenDecode;
        this.objectMapper = objectMapper;
    }

    /**
     * 认证访问令牌，并把用户信息写入认证上下文。
     *
     * @param authentication Bearer 认证请求
     * @return 已认证的 Bearer 认证对象
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        BearerTokenAuthentication tokenAuthentication = (BearerTokenAuthentication) authentication;
        try {
            SignedJWT jwt = (SignedJWT) tokenDecode.decode(tokenAuthentication.getCredentials());
            verifyExpired(jwt);
            verifyAccessToken(jwt);

            UserDetails userDetails = toUserDetails(jwt);
            // Redis 中不存在或值不一致，都视为令牌已被后续登录或退出操作撤销。
            boolean existAccessToken = TokenStorage.isExistAccessToken(String.valueOf(userDetails.getId()));
            if (!existAccessToken) {
                throw new CredentialsExpiredException("令牌已被撤销。");
            }
            boolean accessMatches = TokenStorage.accessMatches(String.valueOf(userDetails.getId()), tokenAuthentication.getCredentials());
            if (!accessMatches) {
                throw new CredentialsExpiredException("令牌已被撤销。");
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
            throw new CredentialsExpiredException("令牌解析失败。");
        }
    }

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
        String tokenType = (String) jwt.getHeader().getCustomParam(SecurityParameterConstant.TokenType.class.getName());
        long now = Calendar.getInstance().getTime().getTime();
        long expired = expirationTime.getTime();
        if (now >= expired) {
            throw new CredentialsExpiredException(getTokenTypeName(tokenType) + "已过期。");
        }
    }

    /**
     * 只允许访问令牌访问受保护接口，刷新令牌不能直接作为 Bearer 凭证使用。
     *
     * @param jwt 已解析的 JWT
     */
    private void verifyAccessToken(SignedJWT jwt) {
        String tokenType = (String) jwt.getHeader().getCustomParam(SecurityParameterConstant.TokenType.class.getName());
        if (!SecurityParameterConstant.TokenType.access_token.name().equals(tokenType)) {
            throw new BadCredentialsException("令牌类型错误");
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
            throw new BadCredentialsException("令牌用户信息不能为空。");
        }
        UserDetails userDetails = objectMapper.convertValue(user, UserDetails.class);
        if (userDetails == null || userDetails.getId() == null) {
            throw new BadCredentialsException("令牌用户标识不能为空。");
        }
        return userDetails;
    }

    private String getTokenTypeName(String tokenType) {
        if (SecurityParameterConstant.TokenType.access_token.name().equals(tokenType)) {
            return "访问令牌";
        }
        if (SecurityParameterConstant.TokenType.refresh_token.name().equals(tokenType)) {
            return "刷新令牌";
        }
        return "令牌";
    }
}
