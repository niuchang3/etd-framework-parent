package com.etd.framework.starter.client;


import cn.hutool.crypto.PemUtil;
import com.etd.framework.starter.client.core.TenantAuthority;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenDecode;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.etd.framework.starter.client.core.user.memory.MemoryUserServiceImpl;
import com.google.common.collect.Lists;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@ComponentScan({"com.etd.framework.starter.client.core.*"})
@EnableConfigurationProperties
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


}
