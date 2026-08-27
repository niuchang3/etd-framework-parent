package com.etd.framework.starter.client.autoconfigure;


import cn.hutool.crypto.PemUtil;
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        RSAPublicKey rsaPublicKey = publicKey(securityProperties);
        return new JwtTokenDecode(rsaPublicKey);
    }

    /**
     * 从配置路径读取 RSA 公钥。
     *
     * @param securityProperties 安全配置
     * @return RSA 公钥
     */
    private RSAPublicKey publicKey(SecurityProperties securityProperties) {
        String location = getPublicKeyLocation(securityProperties);
        try (InputStream inputStream = Files.newInputStream(resolveConfFile(location))) {
            return (RSAPublicKey) PemUtil.readPemPublicKey(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("读取公钥配置失败。", e);
        }
    }

    /**
     * 从配置中获取公钥路径。
     *
     * @param securityProperties 安全配置
     * @return 公钥文件路径
     */
    private String getPublicKeyLocation(SecurityProperties securityProperties) {
        if (securityProperties.getKey() == null || !StringUtils.hasText(securityProperties.getKey().getPublicKeyPath())) {
            return "conf/rsaPublicKey.pem";
        }
        return securityProperties.getKey().getPublicKeyPath();
    }

    /**
     * 解析密钥文件路径。
     * <p>
     * 绝对路径直接使用；相对路径会优先按当前运行目录解析，找不到时再从当前目录向上查找。
     *
     * @param location 配置文件路径
     * @return 配置文件路径
     */
    private Path resolveConfFile(String location) throws IOException {
        Path configured = Paths.get(location);
        if (configured.isAbsolute()) {
            if (Files.exists(configured)) {
                return configured;
            }
            throw new IOException("未找到配置的公钥文件：" + configured);
        }

        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(configured);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("从当前目录向上查找，未找到配置文件 " + location + "，当前目录：" + System.getProperty("user.dir"));
    }

}
