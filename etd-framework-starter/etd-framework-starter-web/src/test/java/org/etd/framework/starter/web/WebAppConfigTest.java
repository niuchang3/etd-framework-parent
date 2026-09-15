package org.etd.framework.starter.web;

import org.etd.framework.common.core.spring.SpringContextHelper;
import org.etd.framework.starter.web.config.StarterWebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Web Starter 自动配置测试。
 */
class WebAppConfigTest {

    /**
     * 引入 Web Starter 后应自动注册 Spring 上下文访问组件，业务应用无需重复声明导入注解。
     */
    @Test
    void shouldRegisterSpringContextHelper() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(StarterWebConfig.class, WebAppConfig.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(SpringContextHelper.class);
                    assertThat(SpringContextHelper.getApplicationContext()).isNotNull();
                });
    }
}
