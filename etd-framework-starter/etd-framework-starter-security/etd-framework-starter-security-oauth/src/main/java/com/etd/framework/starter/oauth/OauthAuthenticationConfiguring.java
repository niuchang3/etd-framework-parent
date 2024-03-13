package com.etd.framework.starter.oauth;


import cn.hutool.crypto.PemUtil;
import com.etd.framework.starter.client.core.Oauth2AuthenticationConfigurer;
import com.etd.framework.starter.client.core.configurer.BearerAuthenticationConfigurer;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokeEncoder;
import com.etd.framework.starter.oauth.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.oauth.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.oauth.authentication.password.configurer.UserPasswordAuthenticationConfigurer;
import com.etd.framework.starter.oauth.authentication.refresh.configurer.RefreshTokenAuthenticationConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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


        http.apply(configurer)
                .and()
                .csrf().disable()
                .requestCache().disable()
                .requestMatchers().antMatchers("/**")
                .and()
                .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .accessDeniedHandler(new AccessDeniedHandlerImpl())
                .authenticationEntryPoint(new AuthenticationEntryPointImpl());
        return http.build();
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

}
