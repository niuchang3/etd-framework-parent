package com.etd.framework.starter.client;


import cn.hutool.crypto.PemUtil;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenDecode;
import com.etd.framework.starter.client.core.properties.SystemOauthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.interfaces.RSAPublicKey;

@Configuration
@ComponentScan({"com.etd.framework.starter.client.**"})
@EnableConfigurationProperties(value = SystemOauthProperties.class)
public class SecurityClientConfiguring {


    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(TokenDecode.class)
    public TokenDecode tokenDecode() {
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
        try (InputStream inputStream = Files.newInputStream(resolveConfFile("rsaPublicKey.pem"))) {
            return (RSAPublicKey) PemUtil.readPemPublicKey(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path resolveConfFile(String filename) throws IOException {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("conf").resolve(filename);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("Cannot find conf/" + filename + " from " + System.getProperty("user.dir"));
    }


    public static void main(String[] args) {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String encode = encoder.encode("admin");
        System.out.println(encode);
    }

}
