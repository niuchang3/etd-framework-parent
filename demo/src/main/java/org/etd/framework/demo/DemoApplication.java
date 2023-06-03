package org.etd.framework.demo;

import org.etd.framework.common.core.spring.SpringContextHelper;
import org.etd.framework.starter.web.WebAppConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * @author Young
 * @description
 * @date 2020/12/26
 */

@MapperScan("org.etd.framework.demo.mapper")
@Import({WebAppConfig.class, SpringContextHelper.class})
@SpringBootApplication
public class DemoApplication {


    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class);
    }

}
