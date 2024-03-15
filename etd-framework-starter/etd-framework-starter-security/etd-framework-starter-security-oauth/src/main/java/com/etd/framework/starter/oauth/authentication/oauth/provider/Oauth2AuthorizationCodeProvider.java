package com.etd.framework.starter.oauth.authentication.oauth.provider;

import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.oauth.OauthClient;
import com.etd.framework.starter.client.core.oauth.OauthClientService;
import com.etd.framework.starter.client.core.token.Oauth2AuthorizationCodeRequestToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.ObjectUtils;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Oauth2AuthorizationCodeProvider implements AuthenticationProvider {

    private OauthClientService oauthClientService;


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Oauth2AuthorizationCodeRequestToken requestToken = (Oauth2AuthorizationCodeRequestToken) authentication;
        if (StringUtils.isEmpty(requestToken.getClientId())){
            throw new UsernameNotFoundException("invalid_request");
        }

        OauthClient oauthClient = oauthClientService.loadByClientId(requestToken.getClientId());
        validata(oauthClient,requestToken);

        Oauth2AuthorizationCodeRequestToken authorization = new Oauth2AuthorizationCodeRequestToken(null);
        BeanUtils.copyProperties(requestToken,authorization);

        authorization.setAuthenticated(true);
        authorization.setDetails(oauthClient);

        return authorization;
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return Oauth2AuthorizationCodeRequestToken.class.isAssignableFrom(authentication);
    }

    private void validata(OauthClient client, Oauth2AuthorizationCodeRequestToken requestToken) {

        if (ObjectUtils.isEmpty(client)) {
            throw new UsernameNotFoundException("invalid_request");
        }

        if (!requestToken.getRedirectUri().equals(client.getRedirectUri())) {
            throw new UsernameNotFoundException("invalid_request");
        }

        if (Oauth2ParameterConstant.STATUS.Deactivate.name().equals(client.getStatus())) {
            throw new DisabledException("access_denied");
        }
        if (Oauth2ParameterConstant.STATUS.Lock.name().equals(client.getStatus())) {
            throw new LockedException("access_denied");
        }
    }
}
