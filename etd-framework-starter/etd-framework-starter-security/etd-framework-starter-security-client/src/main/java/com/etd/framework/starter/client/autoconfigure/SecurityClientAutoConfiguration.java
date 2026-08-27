package com.etd.framework.starter.client.autoconfigure;


import com.etd.framework.starter.client.core.encrypt.SecurityKeyLoader;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenDecode;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.interfaces.RSAPublicKey;

/**
 * 安全客户端自动配置。
 * <p>
 * 提供密码编码器、JWT 公钥解码器以及客户端侧基础组件扫描。
 */
@AutoConfiguration
@ComponentScan("com.etd.framework.starter.client")
@EnableConfigurationProperties(value = SecurityProperties.class)
public class SecurityClientAutoConfiguration {

    /**
     * 提供 Spring Security 默认密码编码器。
     * <p>
     * 业务系统未自定义 {@link PasswordEncoder} 时，使用带算法前缀的委托编码器。
     *
     * @return 密码编码器
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 提供 JWT 解码器。
     *
     * @return 令牌解码器
     */
    @Bean
    @ConditionalOnMissingBean(TokenDecode.class)
    public TokenDecode tokenDecode(SecurityProperties securityProperties) {
        RSAPublicKey rsaPublicKey = SecurityKeyLoader.loadPublicKey(securityProperties);
        return new JwtTokenDecode(rsaPublicKey);
    }

}
