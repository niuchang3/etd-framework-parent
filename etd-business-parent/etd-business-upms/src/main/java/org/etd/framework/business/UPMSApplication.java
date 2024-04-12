package org.etd.framework.business;

import org.etd.framework.common.core.spring.SpringContextHelper;
import org.etd.framework.starter.web.WebAppConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * User Permissions Management System Application
 */
@MapperScan("org.etd.framework.business.**.mapper")
@EnableWebSecurity
@Import({WebAppConfig.class, SpringContextHelper.class})
@SpringBootApplication
public class UPMSApplication {
    public static void main(String[] args) {
        SpringApplication.run(UPMSApplication.class, args);
    }
}
