package org.etd.ai;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 从各应用真实启动 YAML 加载配置，验证公共模块迁移后的导入、覆盖及资源唯一性。 */
class BusinessSharedResourcesTest {

    /** 不启动外部连接，只执行 Spring Boot 原生 ConfigData 加载各应用及其 local 配置。 */
    @ParameterizedTest
    @CsvSource({
            "etd-business-upms, upms-server, true, true",
            "etd-business-event, etd-event-server, true, false",
            "etd-business-ai-agent, ai-agent, false, true"
    })
    void shouldLoadSharedConfiguration(String module, String applicationName, boolean event, boolean druid)
            throws Exception {
        Path resources = Path.of("..", module, "src", "main", "resources").toAbsolutePath().normalize();
        URL[] locations = {resources.toUri().toURL()};
        ClassLoader parent = getClass().getClassLoader();
        try (URLClassLoader loader = new URLClassLoader(locations, parent);
             GenericApplicationContext context = new GenericApplicationContext()) {
            context.setClassLoader(loader);
            String applicationLocation = resources.resolve("application.yml").toUri().toString();
            MapPropertySource properties = new MapPropertySource("resource-test", Map.of(
                    "spring.config.location", applicationLocation,
                    "spring.profiles.active", "local",
                    "REDIS_HOST", "redis.resource-test.invalid",
                    "KAFKA_SERVERS", "kafka.resource-test.invalid:9092",
                    "KAFKA_CONSUMER_GROUP_ID", "shared-resources-test",
                    "ETD_EVENT_WORKER_ID", "7"));
            context.getEnvironment().getPropertySources().addFirst(properties);
            ConfigDataApplicationContextInitializer initializer = new ConfigDataApplicationContextInitializer();
            initializer.initialize(context);
            verifyEffectiveConfiguration(context, applicationName, event, druid);
            verifySharedResourceLocations(loader, resources);
        }
    }

    /** 确认占位符仍由运行环境解析，应用只加载自身显式导入的组件。 */
    private void verifyEffectiveConfiguration(GenericApplicationContext context, String applicationName,
                                               boolean event, boolean druid) {
        ConfigurableEnvironment environment = context.getEnvironment();
        assertEquals(applicationName, environment.getProperty("spring.application.name"));
        assertEquals("redis.resource-test.invalid", environment.getProperty("spring.data.redis.host"));
        assertEquals(applicationName + ":", environment.getProperty("jetcache.remote.default.keyPrefix"));
        assertEquals("Asia/Shanghai", environment.getProperty("spring.jackson.time-zone"));
        assertEquals(druid, environment.containsProperty("spring.datasource.druid.max-active"));
        assertEquals(event, environment.containsProperty("spring.kafka.bootstrap-servers"));
        if (event) {
            assertEquals("kafka.resource-test.invalid:9092", environment.getProperty("spring.kafka.bootstrap-servers"));
            assertEquals("shared-resources-test", environment.getProperty("spring.kafka.consumer.group-id"));
            assertEquals("7", environment.getProperty("etd.event.bus.client.id-generator.worker-id"));
        } else {
            assertFalse(environment.containsProperty("etd.event.bus.topic"));
            assertTrue(environment.containsProperty("storage.minio.endpoint"));
        }
    }

    /** 共享 YAML 只由 common 提供，各应用不再有副本；SQL 仍通过测试 classpath 读取。 */
    private void verifySharedResourceLocations(ClassLoader loader, Path applicationResources) throws Exception {
        ArrayList<URL> redisResources = Collections.list(loader.getResources("redis.yml"));
        assertEquals(1, redisResources.size());
        assertTrue(redisResources.getFirst().toString().contains("etd-business-common"));
        assertFalse(applicationResources.resolve("redis.yml").toFile().exists());
        assertNotNull(loader.getResource("sql/ai.sql"));
        assertNotNull(loader.getResource("sql/event.sql"));
        assertNotNull(loader.getResource("sql/init.sql"));
    }
}
