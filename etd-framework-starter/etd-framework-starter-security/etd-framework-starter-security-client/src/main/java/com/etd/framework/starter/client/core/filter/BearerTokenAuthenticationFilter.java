package com.etd.framework.starter.client.core.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {


    private AuthenticationConverter converter;

    private AuthenticationManager authenticationManager;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(emptyContext);
            Authentication convert = converter.convert(request);
            if (convert == null) {
                filterChain.doFilter(request, response);
                return;
            }
            Authentication authenticate = authenticationManager.authenticate(convert);
            if(!authenticate.isAuthenticated()){
                throw new CredentialsExpiredException("凭证错误");
            }

            emptyContext.setAuthentication(authenticate);
            SecurityContextHolder.setContext(emptyContext);
            filterChain.doFilter(request, response);
        }finally {
            SecurityContextHolder.clearContext();
        }

    }

}
