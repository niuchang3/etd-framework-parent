package com.etd.framework.starter.oauth.authentication.configurer;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.CookieRequestCache;

public class DefaultOauth2ServerAuthorizationConfigurer {


    public static void applyDefaultOauth2ServerAuthorization(HttpSecurity http) throws Exception {
        Oauth2ServerAuthorizationConfigurer configurer = new Oauth2ServerAuthorizationConfigurer();


        http.requestCache()
                .requestCache(new CookieRequestCache())
                .and()
                .authorizeRequests()
                .antMatchers("/webpage/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .csrf().ignoringRequestMatchers(configurer.getEndpointsMatcher())
                .and()
                .exceptionHandling( exceptionHandling -> {
                    exceptionHandling.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/webpage/login"));
                })
                .formLogin().disable()
                .oauth2Login().disable()
//                .anonymous().disable()
                .apply(configurer);

    }
}
