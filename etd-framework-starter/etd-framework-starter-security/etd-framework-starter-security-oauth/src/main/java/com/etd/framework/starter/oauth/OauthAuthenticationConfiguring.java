package com.etd.framework.starter.oauth;


import cn.hutool.crypto.PemUtil;
import com.etd.framework.starter.client.core.configurer.BearerAuthenticationConfigurer;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokeEncoder;
import com.etd.framework.starter.oauth.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.oauth.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.client.core.Oauth2AuthenticationConfigurer;
import com.etd.framework.starter.oauth.authentication.password.configurer.UserPasswordAuthenticationConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
        configurer.addConfigurer(new BearerAuthenticationConfigurer());


        http.apply(configurer)
                .and()
                .csrf().disable()
                .requestMatchers().antMatchers("/**")
                .and()
                .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .accessDeniedHandler( new AccessDeniedHandlerImpl())
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


/**
 * 默认的用户名密码Service
 *
 * @return
 *//*

    @Bean
    @ConditionalOnMissingBean(UserService.class)
    public UserService defaultUserPasswordService() {
        UserService userService = new MemoryUserPasswordServiceImpl();
        Oauth2UserDetails build = Oauth2UserDetails.builder()
                .id(1L)
                .account("admin")
                .password("123456")
                .userName("牛昌")
                .nickName("淡淡丶奶油味")
                .gender(1)
                .build();


        userService.register(build);

        return userService;
    }

    */
/**
 * 默认的Oauth2客户端信息
 *
 * @return
 *//*

    @Bean
    @ConditionalOnMissingBean(Oauth2ClientService.class)
    public Oauth2ClientService defaultOauth2Client() {
        return new MemoryOauth2ClientServiceImpl();
    }

    */
/**
 * 默认的授权信息服务
 *
 * @return
 *//*

    @Bean
    @ConditionalOnMissingBean(Oauth2AuthorizationService.class)
    public Oauth2AuthorizationService defaultOauth2Authorization() {
        return new MemoryOauth2AuthorizationServiceImpl();
    }

    */
/**
 * 默认的授权同意接口服务
 *
 * @return
 *//*

    @Bean
    @ConditionalOnMissingBean(Oauth2AuthorizationConsentService.class)
    public Oauth2AuthorizationConsentService defaultOauth2AuthorizationConsentService() {
        return new MemoryOauth2AuthorizationConsentServiceImpl();
    }
*/


}
