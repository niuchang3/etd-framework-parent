package com.etd.framework.starter.oauth.authentication.refresh.provider;

import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.storage.TokenStorage;
import com.etd.framework.starter.client.core.token.RefreshTokenRequestToken;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.google.gson.Gson;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {


    private IUserService userService;

    private TokenDecode<SignedJWT> tokenDecode;

    /**
     * 刷新Token只对账号做验证，如重新分配权限需要退出后重新登录
     *
     * @param authentication the authentication request object.
     * @return
     * @throws AuthenticationException
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            SignedJWT jwt = tokenDecode.decode((String) authentication.getCredentials());
            verifyExpired(jwt);
            verifyTokenType(jwt);
            UserDetails jwtUser = toUserDetails(jwt);
            isExist(jwt, jwtUser.getId());

            Oauth2ParameterConstant.TokenNameSpace nameSpace = getNameSpace(jwt);
            boolean existRefreshToken = TokenStorage.isExistRefreshToken(nameSpace.name(), String.valueOf(jwtUser.getId()));
            if (!existRefreshToken) {
                throw new CredentialsExpiredException("Token Has Been Revoked");
            }
            boolean accessMatches = TokenStorage.refreshMatches(nameSpace.name(), String.valueOf(jwtUser.getId()), (String) authentication.getCredentials());
            if (!accessMatches) {
                throw new CredentialsExpiredException("Token Has Been Revoked");
            }
            UserDetails userDetails = userService.loadUserById(jwtUser.getId());
            validata(userDetails);
            RefreshTokenRequestToken newAuthentication = converterAuthentication((String) authentication.getCredentials(), userDetails);
            newAuthentication.setNamespace(nameSpace.name());
            return newAuthentication;

        } catch (JOSEException | ParseException e) {
            throw new BadCredentialsException(e.getMessage(), e);
        }
    }


    private void validata(UserDetails userDetails) {
        if (!userDetails.getEnabled()) {
            throw new DisabledException("账号已被禁用。");
        }
        if (userDetails.getLocked()) {
            throw new LockedException("账号已被锁定,请联系管理员解锁。");
        }
    }

    /**
     * 验证是否过期
     *
     * @param jwt
     * @throws ParseException
     */
    private void verifyExpired(SignedJWT jwt) throws ParseException {
        Date expirationTime = jwt.getJWTClaimsSet().getExpirationTime();
        String tokenType = (String) jwt.getHeader().getCustomParam(Oauth2ParameterConstant.TokenType.class.getName());
        long now = Calendar.getInstance().getTime().getTime();
        long expired = expirationTime.getTime();
        if (now >= expired) {
            throw new CredentialsExpiredException(tokenType + "Token Credentials Expired");
        }
    }

    /**
     * 验证Token 类型
     *
     * @param jwt
     */
    private void verifyTokenType(SignedJWT jwt) {
        JWSHeader header = jwt.getHeader();
        String tokenType = (String) header.getCustomParam(Oauth2ParameterConstant.TokenType.class.getName());
        if (!Oauth2ParameterConstant.TokenType.refresh_token.name().equals(tokenType)) {
            throw new BadCredentialsException("令牌类型错误");
        }

    }

    /**
     * 验证Token是否被吊销
     *
     * @param jwt
     * @param userId
     */
    private void isExist(SignedJWT jwt, Long userId) {

        Oauth2ParameterConstant.TokenNameSpace nameSpace = getNameSpace(jwt);
        boolean existRefreshToken = TokenStorage.isExistRefreshToken(nameSpace.name(), String.valueOf(userId));
        if (!existRefreshToken) {
            throw new CredentialsExpiredException("令牌失效");
        }
    }

    /**
     * 从JWT中获取命名空间
     *
     * @param jwt
     * @return
     */
    private Oauth2ParameterConstant.TokenNameSpace getNameSpace(SignedJWT jwt) {
        String customParam = (String) jwt.getHeader().getCustomParam(Oauth2ParameterConstant.TokenNameSpace.class.getName());
        return Oauth2ParameterConstant.TokenNameSpace.valueOf(customParam);
    }

    /**
     * 用户详情转换为Authentication
     *
     * @param userDetails
     * @return
     */
    private RefreshTokenRequestToken converterAuthentication(String tokenValue, UserDetails userDetails) {
        RefreshTokenRequestToken token = new RefreshTokenRequestToken(userDetails.getAuthorities());
        token.setGrantType(Oauth2ParameterConstant.TokenType.refresh_token.name());
        token.setCredentials(tokenValue);
        token.setDetails(userDetails);
        token.setAuthenticated(true);
        return token;
    }

    private UserDetails toUserDetails(SignedJWT jwt) throws ParseException {
        Object user = jwt.getJWTClaimsSet().getClaim(Authentication.class.getName());
        Gson gson = new Gson();
        String json = gson.toJson(user);
        return gson.fromJson(json, UserDetails.class);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RefreshTokenRequestToken.class.isAssignableFrom(authentication);
    }

}
