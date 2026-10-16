package org.etd.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智能体业务应用入口，按功能域扫描 Mapper，承载 AI 编排、框架数据访问和令牌鉴权能力。
 */
@SpringBootApplication
@MapperScan("org.etd.ai.**.mapper")
public class AgentApplication {

    /**
     * 启动独立的智能体业务应用，模型凭证与存储连接信息由运行环境注入。
     *
     * @param args Spring Boot 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
