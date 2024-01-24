package com.etd.framework.starter.oauth.authentication.filter.extend;

import com.etd.framework.starter.oauth.authentication.filter.AbstractOauth2RequestFilter;
import org.springframework.core.log.LogMessage;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginAuthorizeRequestFilter extends AbstractOauth2RequestFilter {

    private AuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();

    private AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();

    @Override
    protected void sendAccessTokenResponse(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
//        this.securityContextRepository.saveContext(context, request, response);
//        if (this.logger.isDebugEnabled()) {
//            this.logger.debug(LogMessage.format("Set SecurityContextHolder to %s", authentication));
//        }
//        this.rememberMeServices.loginSuccess(request, response, authentication);
//        if (this.eventPublisher != null) {
//            this.eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authentication, this.getClass()));
//        }
        this.successHandler.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    protected void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws ServletException, IOException {
        failureHandler.onAuthenticationFailure(request,response,exception);
    }
}
