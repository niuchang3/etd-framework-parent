package org.etd.framework.business;

import org.etd.framework.common.core.spring.SpringContextHelper;
import org.etd.framework.starter.web.WebAppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Hello world!
 */
@EnableWebSecurity
@Import({WebAppConfig.class, SpringContextHelper.class})
@SpringBootApplication
public class UserPermissionsManagementSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserPermissionsManagementSystemApplication.class, args);
    }
}
