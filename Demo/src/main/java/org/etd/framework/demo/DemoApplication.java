package org.etd.framework.demo;

import org.etd.framework.common.core.spring.SpringContextHelper;
import org.etd.framework.starter.web.WebAppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * @author Young
 * @description
 * @date 2020/12/26
 */
@Import({WebAppConfig.class, SpringContextHelper.class})
@SpringBootApplication
public class DemoApplication {



    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class);
    }
}
