package com.etd.framework.starter.oauth.authentication.internal.provider;

import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.storage.TokenStorage;
import com.etd.framework.starter.client.core.user.IUserService;
import org.etd.framework.common.core.user.UserDetails;
import com.etd.framework.starter.oauth.authentication.internal.token.RefreshTokenRequestToken;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * 刷新令牌认证提供者。
 * <p>
 * 负责校验刷新令牌签名、类型、有效期、服务端存储状态，并重新加载当前用户状态。
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {


    @Setter(onMethod_ = @Autowired)
    private IUserService userService;

    @Setter(onMethod_ = @Autowired)
    private TokenDecode<SignedJWT> tokenDecode;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(userService, "用户服务不能为空。");
        Assert.notNull(tokenDecode, "令牌解码器不能为空。");
        RefreshTokenRequestToken requestToken = (RefreshTokenRequestToken) authentication;
        validateRequest(requestToken);
        try {
            SignedJWT jwt = tokenDecode.decode((String) requestToken.getCredentials());
            verifyExpired(jwt);
            verifyTokenType(jwt);
            UserDetails jwtUser = toUserDetails(jwt);
            verifyStorage(jwtUser.getId(), (String) requestToken.getCredentials());
            UserDetails userDetails = userService.loadUserById(jwtUser.getId());
            validate(userDetails);
            return converterAuthentication((String) requestToken.getCredentials(), userDetails);

        } catch (JOSEException | ParseException e) {
            throw new BadCredentialsException("令牌解析失败。", e);
        }
    }

    /**
     * 验证刷新请求参数。
     *
     * @param requestToken 刷新请求
     */
    private void validateRequest(RefreshTokenRequestToken requestToken) {
        if (ObjectUtils.isEmpty(requestToken.getCredentials())) {
            throw new BadCredentialsException("刷新令牌不能为空。");
        }
    }

    /**
     * 验证当前账号状态。
     *
     * @param userDetails 用户详情
     */
    private void validate(UserDetails userDetails) {
        if (ObjectUtils.isEmpty(userDetails)) {
            throw new BadCredentialsException("令牌用户不存在。");
        }
        if (!Boolean.TRUE.equals(userDetails.getEnabled())) {
            throw new DisabledException("账号已被禁用。");
        }
        if (Boolean.TRUE.equals(userDetails.getLocked())) {
            throw new LockedException("账号已被锁定，请联系管理员解锁。");
        }
    }

    /**
     * 验证令牌是否过期。
     *
     * @param jwt 已解析的 JWT
     * @throws ParseException
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
     * 验证令牌类型。
     *
     * @param jwt 已解析的 JWT
     */
    private void verifyTokenType(SignedJWT jwt) {
        String tokenType = (String) jwt.getHeader().getCustomParam(SecurityParameterConstant.TokenType.class.getName());
        if (!SecurityParameterConstant.TokenType.refresh_token.name().equals(tokenType)) {
            throw new BadCredentialsException("令牌类型错误");
        }

    }

    /**
     * 验证刷新令牌是否仍是服务端保存的当前令牌。
     *
     * @param userId 用户标识
     * @param tokenValue 刷新令牌
     */
    private void verifyStorage(Long userId, String tokenValue) {
        boolean existRefreshToken = TokenStorage.isExistRefreshToken(String.valueOf(userId));
        if (!existRefreshToken) {
            throw new CredentialsExpiredException("令牌已被撤销。");
        }
        boolean refreshMatches = TokenStorage.refreshMatches(String.valueOf(userId), tokenValue);
        if (!refreshMatches) {
            throw new CredentialsExpiredException("令牌已被撤销。");
        }
    }

    /**
     * 用户详情转换为认证结果。
     *
     * @param tokenValue 刷新令牌
     * @param userDetails 用户详情
     * @return 认证结果
     */
    private RefreshTokenRequestToken converterAuthentication(String tokenValue, UserDetails userDetails) {
        RefreshTokenRequestToken token = new RefreshTokenRequestToken(userDetails.getAuthorities());
        token.setCredentials(tokenValue);
        token.setPrincipal(String.valueOf(userDetails.getId()));
        token.setDetails(userDetails);
        token.setAuthenticated(true);
        return token;
    }

    /**
     * 从 JWT 载荷中还原用户标识。
     *
     * @param jwt 已解析的 JWT
     * @return JWT 中的用户信息
     */
    private UserDetails toUserDetails(SignedJWT jwt) throws ParseException {
        String subject = jwt.getJWTClaimsSet().getSubject();
        if (ObjectUtils.isEmpty(subject)) {
            throw new BadCredentialsException("令牌用户标识不能为空。");
        }
        try {
            UserDetails userDetails = new UserDetails();
            userDetails.setId(Long.valueOf(subject));
            return userDetails;
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("令牌用户标识格式错误。", e);
        }
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

    @Override
    public boolean supports(Class<?> authentication) {
        return RefreshTokenRequestToken.class.isAssignableFrom(authentication);
    }

}
