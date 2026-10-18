package org.etd.ai.knowledge;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import jakarta.validation.Validation;
import org.etd.ai.knowledge.controller.dto.KnowledgeFolderSaveDTO;
import org.etd.ai.knowledge.converter.KnowledgeFolderConverter;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.mapper.KnowledgeFolderMapper;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.MybatisConfig;
import org.etd.framework.starter.mybaits.fill.handler.extend.DefaultDataFillHandler;
import org.h2.jdbcx.JdbcDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.support.StaticApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** 加载真实目录建表定义及框架配置，验证无组织目录的读写和租户隔离。 */
class KnowledgeFolderPersistenceTest {
    /** 为实际自动填充表达式提供Spring容器。 */
    private final StaticApplicationContext applicationContext = new StaticApplicationContext();

    /** 避免请求上下文污染后续测试。 */
    @AfterEach
    void clearContext() {
        RequestContext.clean();
        applicationContext.close();
    }

    /** 描述可保存和清空；没有组织及角色数据范围也可维护本租户目录。 */
    @Test
    void shouldPersistSharedFolderWithoutOrganization() throws Exception {
        var sessionFactory = createSessionFactory();
        var user = new UserDetails();
        user.setId(100L);
        RequestContext.setUser(user);
        RequestContext.setTenantCode(1L);
        try (var session = sessionFactory.openSession(true)) {
            var mapper = session.getMapper(KnowledgeFolderMapper.class);
            var entity = createFolder(mapper);
            user.setId(101L);
            var saved = mapper.selectById(entity.getId());
            assertEquals("产品资料分类说明", saved.getDescription());
            var response = KnowledgeFolderConverter.INSTANCE.toVO(saved);
            assertEquals(saved.getDescription(), response.getDescription());
            assertTrue(response.getWritable());
            saved.setDescription(null);
            assertEquals(1, mapper.updateById(saved));
            assertNull(mapper.selectById(saved.getId()).getDescription());
            RequestContext.setTenantCode(2L);
            assertNull(mapper.selectById(saved.getId()));
        }
    }

    /** 使用当前目录DDL创建隔离数据库，并装配实际MyBatis拦截链。 */
    private SqlSessionFactory createSessionFactory() throws Exception {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:folder;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        createFolderTable(dataSource);
        var configuration = new MybatisConfiguration();
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        var globalConfig = new GlobalConfig();
        var fillHandler = new DefaultDataFillHandler();
        fillHandler.setApplicationContext(applicationContext);
        globalConfig.setMetaObjectHandler(fillHandler);
        factory.setGlobalConfig(globalConfig);
        var properties = loadApplicationProperties();
        var framework = new MybatisConfig(properties);
        var interceptor = framework.mybatisPlusInterceptor();
        factory.setPlugins(interceptor);
        var sessionFactory = factory.getObject();
        sessionFactory.getConfiguration().addMapper(KnowledgeFolderMapper.class);
        return sessionFactory;
    }

    /** 直接加载目录建表片段，避免测试自行维护另一套字段。 */
    private void createFolderTable(JdbcDataSource dataSource) throws Exception {
        var schema = new ClassPathResource("sql/postgresql/schema.sql");
        String ddl = schema.getContentAsString(StandardCharsets.UTF_8);
        int start = ddl.indexOf("create table ai_knowledge_folder\n");
        int end = ddl.indexOf(";", start);
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(ddl.substring(start, end));
        }
    }

    /** 创建不包含组织字段的目录，默认值交由框架填充。 */
    private KnowledgeFolderEntity createFolder(KnowledgeFolderMapper mapper) {
        var entity = new KnowledgeFolderEntity();
        entity.setFolderName("产品资料");
        entity.setDescription("产品资料分类说明");
        assertEquals(1, mapper.insert(entity));
        return entity;
    }

    /** 使用应用真实YAML绑定，验证目录排除组织过滤而保留租户过滤。 */
    private EtdMyBatisPlusProperties loadApplicationProperties() {
        var yaml = new YamlPropertiesFactoryBean();
        var resource = new ClassPathResource("etd-ai-agent.yml");
        yaml.setResources(resource);
        var source = new PropertiesPropertySource("ai-test", yaml.getObject());
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(source);
        return Binder.get(environment).bind("etd.mybatis", EtdMyBatisPlusProperties.class).get();
    }

    /** 描述选填且长度上限与建表定义保持一致。 */
    @Test
    void shouldValidateDescriptionLength() {
        var dto = new KnowledgeFolderSaveDTO();
        dto.setFolderName("产品资料");
        dto.setSortOrder(0);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(dto).isEmpty());
            dto.setDescription("文".repeat(500));
            assertTrue(validator.validate(dto).isEmpty());
            dto.setDescription("文".repeat(501));
            assertFalse(validator.validate(dto).isEmpty());
        }
    }
}
