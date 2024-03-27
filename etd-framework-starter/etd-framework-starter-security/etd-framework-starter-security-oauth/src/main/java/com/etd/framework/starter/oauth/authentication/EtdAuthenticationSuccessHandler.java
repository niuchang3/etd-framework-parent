package com.etd.framework.starter.oauth.authentication;

import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.ObjectUtils;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class EtdAuthenticationSuccessHandler extends AbstractAuthenticationHandler implements AuthenticationSuccessHandler {


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        ResultModel<Authentication> success = ResultModel.success(authentication);
        if(ObjectUtils.isEmpty(request.getCookies())){
            writeSuccess(response, success);
            return;
        }
        for (Cookie cookie : request.getCookies()) {
            if(Oauth2ParameterConstant.CookieName.REDIRECT_URL.name().equals(cookie.getName())){
                Cookie responseCookie = new Cookie(Oauth2ParameterConstant.CookieName.REDIRECT_URL.name(),"");
                responseCookie.setMaxAge(0);
                responseCookie.setPath("/");
                responseCookie.setMaxAge(0);
                responseCookie.setHttpOnly(true);
                response.addCookie(responseCookie);
                response.sendRedirect(cookie.getValue());
                return;
            }
        }
        writeSuccess(response, success);

    }
}
