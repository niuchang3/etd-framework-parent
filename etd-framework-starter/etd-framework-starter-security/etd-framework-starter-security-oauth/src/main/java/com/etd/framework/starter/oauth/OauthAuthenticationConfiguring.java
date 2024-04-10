package com.etd.framework.starter.oauth;


import cn.hutool.crypto.PemUtil;
import com.etd.framework.starter.client.core.Oauth2AuthenticationConfigurer;
import com.etd.framework.starter.client.core.TenantAuthority;
import com.etd.framework.starter.client.core.configurer.BearerAuthenticationConfigurer;
import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokeEncoder;
import com.etd.framework.starter.client.core.oauth.OauthClient;
import com.etd.framework.starter.client.core.oauth.OauthClientService;
import com.etd.framework.starter.client.core.oauth.memory.OauthClientServiceImpl;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.etd.framework.starter.client.core.user.memory.MemoryUserServiceImpl;
import com.etd.framework.starter.oauth.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.oauth.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.oauth.authentication.oauth.configurer.Oauth2AuthorizationCodeConfigurer;
import com.etd.framework.starter.oauth.authentication.password.configurer.UserPasswordAuthenticationConfigurer;
import com.etd.framework.starter.oauth.authentication.refresh.configurer.RefreshTokenAuthenticationConfigurer;
import com.google.common.collect.Lists;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;

@Configuration
@ComponentScan({"com.etd.framework.starter.oauth.*"})
@EnableConfigurationProperties
public class OauthAuthenticationConfiguring {


    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultAuthenticationServer(HttpSecurity http) throws Exception {
        Oauth2AuthenticationConfigurer configurer = new Oauth2AuthenticationConfigurer();
        configurer.addConfigurer(new UserPasswordAuthenticationConfigurer());
        configurer.addConfigurer(new RefreshTokenAuthenticationConfigurer());
        configurer.addConfigurer(new BearerAuthenticationConfigurer());
        configurer.addConfigurer(new Oauth2AuthorizationCodeConfigurer());


        http.apply(configurer)
                .and()
                .headers().frameOptions().sameOrigin()
                .and()
                .csrf().disable()
//                .anonymous().disable()
                .requestCache().disable()
                .requestMatchers().antMatchers("/**")
                .and()
                .authorizeRequests()
                .antMatchers("/public/**",
                        "/druid/**",
                        "h2-console/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .accessDeniedHandler(new AccessDeniedHandlerImpl())
                .authenticationEntryPoint(new AuthenticationEntryPointImpl());

        DefaultSecurityFilterChain build = http.build();
        return build;
    }

    @Bean
    public JwtTokeEncoder tokeEncoder() {
        PrivateKey privateKey = privateKey();
        return new JwtTokeEncoder(privateKey);
    }

    /**
     * 读取私钥
     *
     * @return
     */

    private PrivateKey privateKey() {
        // 此处最终需要替换到yaml文件内进行配置
        String privateKeyPath = System.getProperty("user.dir") + File.separator + "conf" + File.separator + "rsaPrivateKey.pem";
        try (InputStream inputStream = Files.newInputStream(Paths.get(privateKeyPath))) {
            return PemUtil.readPemPrivateKey(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Bean
    @ConditionalOnMissingBean(IUserService.class)
    public IUserService userService(PasswordEncoder passwordEncoder) {


        TenantAuthority addUserAuthority = TenantAuthority
                .builder()
                .id(1L)
                .tenantId(1L)
                .authorityName("添加用户")
                .authority("user:manager:add")
                .build();


        IUserService service = new MemoryUserServiceImpl();
        UserDetails userDetails1 = UserDetails.builder()
                .id(1L)
                .account("admin")
                .password(passwordEncoder.encode("admin"))
                .userName("奶油味")
                .nickName("淡淡丶奶油味")
                .enabled(true)
                .locked(false)
                .authorities(Lists.newArrayList(addUserAuthority))
                .build();


        TenantAuthority updateUserAuthority = TenantAuthority
                .builder()
                .id(1L)
                .tenantId(1L)
                .authorityName("添加用户")
                .authority("user:manager:update")
                .build();


        UserDetails userDetails2 = UserDetails.builder()
                .id(2L)
                .account("niuchang")
                .password(passwordEncoder.encode("niuchang"))
                .userName("草莓味")
                .nickName("淡淡丶草莓味")
                .enabled(true)
                .locked(false)
                .authorities(Lists.newArrayList(updateUserAuthority))
                .build();

        service.register(userDetails1);
        service.register(userDetails2);
        return service;
    }

    @Bean
    @ConditionalOnMissingBean(OauthClientService.class)
    public OauthClientService oauthClientService(PasswordEncoder passwordEncoder) {
        OauthClient oauthClient = OauthClient.builder()
                .id(1L)
                .clientName("XXX租户")
                .clientId("niuchangchang")
                .clientPassword(passwordEncoder.encode("123456"))
                .clientDescription("XXX测试租户")
                .redirectUri("www.baidu.com")
                .status(Oauth2ParameterConstant.STATUS.Enable.name())
                .build();
        OauthClientService oauthClientService = new OauthClientServiceImpl();
        oauthClientService.register(oauthClient);
        return oauthClientService;
    }

}
