package org.etd.ai;

import org.etd.ai.model.controller.ModelController;
import org.etd.ai.model.controller.ModelConnectionController;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/** 加载应用及真实资源导入，验证新增目录及租户连接器能够完成启动装配。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=smoke",
        "spring.datasource.druid.url=jdbc:h2:mem:ai-startup;MODE=PostgreSQL",
        "spring.datasource.druid.driver-class-name=org.h2.Driver",
        "spring.datasource.druid.username=sa",
        "spring.datasource.druid.password=",
        "management.health.redis.enabled=false",
        "logging.file=${java.io.tmpdir}/etd-ai-startup-test"
})
class AgentApplicationSmokeTest {
    /** 实际装配的只读模型目录入口。 */
    @Autowired
    private ModelController models;

    /** 实际装配的租户连接器入口。 */
    @Autowired
    private ModelConnectionController connections;

    /** Mapper扫描应包含新平台目录。 */
    @Autowired
    private ModelMapper mapper;

    /** 由实际YAML导入绑定的框架隔离配置。 */
    @Autowired
    private EtdMyBatisPlusProperties properties;

    /** 应用实际注册的路由，检查旧接口不会被残留类重新装配。 */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    /** 全局目录忽略租户与组织，连接器保留租户隔离。 */
    @Test
    void shouldStartWithSharedCatalogAndTenantConnections() {
        assertNotNull(models);
        assertNotNull(connections);
        assertNotNull(mapper);
        for (var mapping : mappings.getHandlerMethods().keySet()) {
            for (var path : mapping.getPatternValues()) {
                assertFalse(path.startsWith("/v1/model-configs"));
            }
        }
        assertTrue(properties.getTenant().getIgnoreTables().contains("ai_model"));
        assertFalse(properties.getTenant().getIgnoreTables().contains("ai_model_connection"));
        assertTrue(properties.getDataPermission().getIgnoreTables().contains("ai_model"));
    }
}
