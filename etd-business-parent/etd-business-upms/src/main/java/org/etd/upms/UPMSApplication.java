package org.etd.upms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User Permissions Management System Application
 */
@MapperScan("org.etd.upms.**.mapper")
@SpringBootApplication
public class UPMSApplication {
    /**
     * main
     *
     * @param args 参数 args
     * @return 处理结果
     */
    public static void main(String[] args) {
        SpringApplication.run(UPMSApplication.class, args);
    }
}
