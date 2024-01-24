package com.etd.framework.starter.oauth.authentication.converter.extend;

import com.etd.framework.starter.oauth.authentication.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.oauth.authentication.converter.AbstractAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.token.LoginAuthenticationToken;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

public class LoginAuthenticationConverter extends AbstractAuthenticationConverter {

    /**
     * 通过Password方式进行登录
     *
     * @return
     */
    @Override
    protected String getGrantType() {
        return Oauth2ParameterConstant.GRANT_TYPE.login_password.name();
    }

    @Override
    protected Authentication doConvert(HttpServletRequest request) {

        String clientId = obtainRequestParams(request, Oauth2ParameterConstant.LoginAuthentication.username.name());
        String clientSecret = obtainRequestParams(request, Oauth2ParameterConstant.LoginAuthentication.password.name());
        String captcha = obtainRequestParams(request, Oauth2ParameterConstant.LoginAuthentication.captcha.name());
        LoginAuthenticationToken authenticationToken = new LoginAuthenticationToken(Collections.emptyList(), clientId, clientSecret);
        authenticationToken.setCaptcha(captcha);
        return authenticationToken;
    }
}
