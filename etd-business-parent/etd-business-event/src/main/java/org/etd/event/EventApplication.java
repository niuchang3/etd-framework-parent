package org.etd.event;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 事件中心业务应用入口，负责承载事件持久化、订阅管理和投递治理能力。
 */
@MapperScan("org.etd.event.**.mapper")
@SpringBootApplication
public class EventApplication {

    /**
     * 启动事件中心业务应用。
     *
     * @param args 应用启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(EventApplication.class, args);
    }
}
