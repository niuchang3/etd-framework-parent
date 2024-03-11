package com.etd.framework.starter.oauth.authentication.password.provider;

import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
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
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.text.ParseException;

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
            JWSHeader header = jwt.getHeader();
            String tokenType = (String) header.getCustomParam(Oauth2ParameterConstant.TokenType.class.getName());
            if(!Oauth2ParameterConstant.TokenType.refresh_token.name().equals(tokenType)){
                throw new BadCredentialsException("wrong Token Type");
            }
            Object user = jwt.getJWTClaimsSet().getClaim(Authentication.class.getName());
            Gson gson = new Gson();
            String json = gson.toJson(user);
            UserDetails userDetails = gson.fromJson(json, UserDetails.class);

            UserDetails newUserDetails = userService.loadUserById(userDetails.getId());
            validata(newUserDetails);
            newUserDetails.setAuthorities(userDetails.getAuthorities());
            return converterAuthentication((String) authentication.getCredentials(), newUserDetails);

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
     * 用户详情转换为Authentication
     *
     * @param userDetails
     * @return
     */
    private Authentication converterAuthentication(String tokenValue, UserDetails userDetails) {
        RefreshTokenRequestToken token = new RefreshTokenRequestToken(userDetails.getAuthorities());
        token.setGrantType(Oauth2ParameterConstant.TokenType.refresh_token.name());
        token.setCredentials(tokenValue);
        token.setDetails(userDetails);
        token.setAuthenticated(true);
        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RefreshTokenRequestToken.class.isAssignableFrom(authentication);
    }

}
