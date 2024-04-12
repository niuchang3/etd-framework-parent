package com.etd.framework.starter.client;


import cn.hutool.crypto.PemUtil;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenDecode;
import com.etd.framework.starter.client.core.properties.SystemOauthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.interfaces.RSAPublicKey;

@Configuration
@ComponentScan({"com.etd.framework.starter.client.**"})
@EnableConfigurationProperties(value = SystemOauthProperties.class)
public class SecurityClientConfiguring {


    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public JwtTokenDecode tokenDecode() {
        RSAPublicKey rsaPublicKey = publicKey();
        return new JwtTokenDecode(rsaPublicKey);
    }

    /**
     * 读取公钥证书
     * 证书位置后续可以写到具体的配置文件内
     *
     * @return
     */

    private RSAPublicKey publicKey() {
        String rsaPublicKeyPath = System.getProperty("user.dir") + File.separator + "conf" + File.separator + "rsaPublicKey.pem";
        try (InputStream inputStream = Files.newInputStream(Paths.get(rsaPublicKeyPath))) {
            return (RSAPublicKey) PemUtil.readPemPublicKey(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String encode = encoder.encode("admin");
        System.out.println(encode);
    }

}
