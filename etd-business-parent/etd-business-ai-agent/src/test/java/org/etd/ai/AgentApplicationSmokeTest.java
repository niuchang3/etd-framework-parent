package org.etd.ai;

import org.etd.ai.model.controller.ModelController;
import org.etd.ai.agent.controller.AgentController;
import org.etd.ai.agent.controller.AgentOptionController;
import org.etd.ai.model.controller.ModelSquareController;
import org.etd.ai.model.biz.ModelSquareBizService;

import java.util.HashSet;
import java.util.Set;
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
    /** Agent管理入口必须在实际业务应用中完成装配。 */
    @Autowired
    private AgentController agents;

    /** Agent选择项入口必须复用业务模型和知识库能力完成装配。 */
    @Autowired
    private AgentOptionController agentOptions;

    /** 实际装配的只读模型管理入口。 */
    @Autowired
    private ModelController models;

    /** 实际装配的首页聚合入口。 */
    @Autowired
    private ModelSquareController square;

    /** 广场必须通过Biz层组合底层模型及连接能力。 */
    @Autowired
    private ModelSquareBizService squareBiz;

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
        assertNotNull(agents);
        assertNotNull(agentOptions);
        assertNotNull(square);
        assertNotNull(squareBiz);
        var registeredPaths = new HashSet<String>();
        assertNotNull(mapper);
        for (var mapping : mappings.getHandlerMethods().keySet()) {
            for (var path : mapping.getPatternValues()) {
                registeredPaths.add(path);
                assertFalse(path.startsWith("/v1/model-configs"));
                assertFalse(path.startsWith("/v1/model-connections"));
            }
        }
        var squarePaths = Set.of("/v1/model-square/models", "/v1/model-square/models/{id}",
                "/v1/model-square/connections", "/v1/model-square/connections/{id}",
                "/v1/model-square/connections/{id}/status");
        assertTrue(registeredPaths.containsAll(squarePaths));
        var agentPaths = Set.of("/v1/agents", "/v1/agents/{id}", "/v1/agents/{id}/configuration",
                "/v1/agents/{id}/publish", "/v1/agents/{id}/status",
                "/v1/agents/options/models", "/v1/agents/options/knowledge-bases");
        assertTrue(registeredPaths.containsAll(agentPaths));
        assertTrue(properties.getTenant().getIgnoreTables().contains("ai_model"));
        assertFalse(properties.getTenant().getIgnoreTables().contains("ai_model_connection"));
        assertTrue(properties.getDataPermission().getIgnoreTables().contains("ai_model"));
    }
}
