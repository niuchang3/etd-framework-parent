package com.etd.framework;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication
public class AuthorizationServer {

    public static void main(String[] args) {

        SpringApplication.run(AuthorizationServer.class);
    }
}
