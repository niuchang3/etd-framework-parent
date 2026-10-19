package org.etd.ai.knowledge;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import jakarta.validation.Validation;
import org.etd.ai.knowledge.controller.dto.KnowledgeFolderSaveDTO;
import org.etd.ai.knowledge.converter.KnowledgeFolderConverter;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.mapper.KnowledgeFolderMapper;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.framework.starter.mybaits.permission.aspect.DataPermissionAspect;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import java.util.Set;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.MybatisConfig;
import org.etd.framework.starter.mybaits.fill.handler.extend.DefaultDataFillHandler;
import org.h2.jdbcx.JdbcDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.support.StaticApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** 加载真实目录建表定义及框架配置，验证前端部门归属、目录分组和租户隔离。 */
class KnowledgeFolderPersistenceTest {
    /** 为实际自动填充表达式提供Spring容器。 */
    private final StaticApplicationContext applicationContext = new StaticApplicationContext();

    /** 避免请求上下文污染后续测试。 */
    @AfterEach
    void clearContext() {
        RequestContext.clean();
        applicationContext.close();
    }

    /** 描述可保存和清空；前端部门归属不受用户主部门覆盖。 */
    @Test
    void shouldPersistSelectedOrganizationWithoutOverriding() throws Exception {
        var sessionFactory = createSessionFactory();
        var user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode()));
        user.setScopeOrgIds(Set.of(777L));
        RequestContext.setUser(user);
        RequestContext.setTenantCode(1L);
        try (var session = sessionFactory.openSession(true)) {
            var mapper = session.getMapper(KnowledgeFolderMapper.class);
            var entity = createFolder(mapper);
            user.setId(101L);
            var saved = mapper.selectById(entity.getId());
            assertEquals(777L, saved.getOrgId());
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

    /** 两个部门的根目录分别查询，不自动带出其他部门或其下级部门数据。 */
    @Test
    @EnabledIfEnvironmentVariable(named = "AI_FOLDER_TEST_JDBC_URL", matches = ".+")
    void shouldSelectOnlyRequestedOrganization() throws Exception {
        var dataSource = new DriverManagerDataSource();
        dataSource.setUrl(System.getenv("AI_FOLDER_TEST_JDBC_URL"));
        dataSource.setUsername(System.getenv("AI_FOLDER_TEST_DB_USER"));
        dataSource.setPassword(System.getenv("AI_FOLDER_TEST_DB_PASSWORD"));
        var sessionFactory = createSessionFactory(dataSource);
        var user = new UserDetails();
        user.setId(100L);
        RequestContext.setUser(user);
        RequestContext.setTenantCode(9001L);
        user.setTenantAdmin(true);
        try (var session = sessionFactory.openSession(true)) {
            var mapper = session.getMapper(KnowledgeFolderMapper.class);
            verifyOrganizationTreeQueries(mapper);
        }
    }

    /** 验证目录树、普通角色读写及两种管理员的隔离边界。 */
    private void verifyOrganizationTreeQueries(KnowledgeFolderMapper mapper) {
        var first = createFolder(mapper);
        var second = new KnowledgeFolderEntity();
        second.setOrgId(888L);
        second.setFolderName("其他部门资料");
        mapper.insert(second);
        RequestContext.getUser().setId(201L);
        var child = new KnowledgeFolderEntity();
        child.setOrgId(777L);
        child.setParentId(first.getId());
        child.setFolderName("子目录");
        mapper.insert(child);
        var service = createFolderService(mapper);
        assertEquals(2, service.fetchFolderList(777L, null).size());
        assertEquals(1L, mapper.selectAncestorCount(first.getId(), child.getId()));
        assertEquals(1L, mapper.selectChildCount(first.getId()));
        verifyOrganizationScope(service, second);
        verifySelfScope(service, first, child);
        verifyAdministratorScopes(service, mapper, second);
    }

    /** 普通组织角色仅能读取和维护授权部门；无数据范围时返回空。 */
    private void verifyOrganizationScope(KnowledgeFolderService service, KnowledgeFolderEntity other) {
        var user = RequestContext.getUser();
        user.setTenantAdmin(false);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode()));
        user.setScopeOrgIds(Set.of(777L));
        assertEquals(2, service.fetchFolderList(777L, null).size());
        assertEquals(2, service.fetchFolderList(null, null).size());
        assertTrue(service.fetchFolderList(888L, null).isEmpty());
        assertThrows(ApiRuntimeException.class, () -> service.requireWritable(other.getId()));
        var requested = new KnowledgeFolderEntity();
        requested.setOrgId(888L);
        requested.setFolderName("越权新增");
        assertThrows(ApiRuntimeException.class, () -> service.createFolder(requested));
        other.setFolderName("不允许的修改");
        assertThrows(ApiRuntimeException.class, () -> service.modifyFolder(other, other.getVersion()));
        user.setPermissionTypes(Set.of());
        assertTrue(service.fetchFolderList(777L, null).isEmpty());
        assertTrue(service.fetchFolderList(null, null).isEmpty());
        assertThrows(ApiRuntimeException.class, () -> service.createFolder(requested));
    }

    /** 本人角色可维护自己的子目录，隐藏祖先仅提供路径且不输出描述和版本。 */
    private void verifySelfScope(KnowledgeFolderService service, KnowledgeFolderEntity root,
            KnowledgeFolderEntity child) {
        var user = RequestContext.getUser();
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode()));
        user.setScopeOrgIds(Set.of());
        var rows = service.fetchFolderList(null, null);
        assertEquals(2, rows.size());
        var ancestor = KnowledgeFolderConverter.INSTANCE.toVO(rows.getFirst());
        assertTrue(ancestor.getNavigationOnly());
        assertFalse(ancestor.getWritable());
        assertNull(ancestor.getVersion());
        assertNull(ancestor.getDescription());
        assertNotNull(service.requireWritable(child.getId()));
        assertThrows(ApiRuntimeException.class, () -> service.requireWritable(root.getId()));
    }

    /** 租户管理员保留租户隔离；平台管理员可读取其他租户记录。 */
    private void verifyAdministratorScopes(KnowledgeFolderService service, KnowledgeFolderMapper mapper,
            KnowledgeFolderEntity other) {
        var user = RequestContext.getUser();
        user.setTenantAdmin(true);
        assertEquals(1, service.fetchFolderList(888L, null).size());
        assertEquals(3, service.fetchFolderList(null, null).size());
        assertNotNull(service.requireWritable(other.getId()));
        RequestContext.setTenantCode(9002L);
        var outside = createFolder(mapper);
        RequestContext.setTenantCode(9001L);
        assertThrows(ApiRuntimeException.class, () -> service.requireWritable(outside.getId()));
        assertEquals(3, service.fetchFolderList(null, null).size());
        user.setTenantAdmin(false);
        user.setPlatformAdmin(true);
        assertNotNull(service.requireWritable(outside.getId()));
        assertEquals(3, service.fetchFolderList(777L, null).size());
        assertEquals(4, service.fetchFolderList(null, null).size());
    }

    /** 装配真实数据权限切面，避免仅调用Mapper绕过Service声明的过滤作用域。 */
    private KnowledgeFolderService createFolderService(KnowledgeFolderMapper mapper) {
        var target = new KnowledgeFolderService(mapper);
        var proxyFactory = new AspectJProxyFactory(target);
        var aspect = new DataPermissionAspect();
        proxyFactory.addAspect(aspect);
        return proxyFactory.getProxy();
    }

    /** 使用当前目录DDL创建隔离数据库，并装配实际MyBatis拦截链。 */
    private SqlSessionFactory createSessionFactory() throws Exception {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:folder" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        createFolderTable(dataSource);
        return createSessionFactory(dataSource);
    }

    /** 同一框架配置可装配H2写入测试及真实PostgreSQL递归查询测试。 */
    private SqlSessionFactory createSessionFactory(DataSource dataSource) throws Exception {
        var configuration = new MybatisConfiguration();
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        var mapperResource = new ClassPathResource("mapper/knowledge/KnowledgeFolderMapper.xml");
        factory.setMapperLocations(mapperResource);
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
        return sessionFactory;
    }

    /** 直接加载目录建表片段，避免测试自行维护另一套字段。 */
    private void createFolderTable(JdbcDataSource dataSource) throws Exception {
        var schema = new ClassPathResource("sql/ai.sql");
        String ddl = schema.getContentAsString(StandardCharsets.UTF_8);
        int start = ddl.indexOf("create table ai_knowledge_folder\n");
        int end = ddl.indexOf(";", start);
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(ddl.substring(start, end));
        }
    }

    /** 创建前端指定部门的目录，其他默认值由框架填充。 */
    private KnowledgeFolderEntity createFolder(KnowledgeFolderMapper mapper) {
        var entity = new KnowledgeFolderEntity();
        entity.setOrgId(777L);
        entity.setFolderName("产品资料");
        entity.setDescription("产品资料分类说明");
        assertEquals(1, mapper.insert(entity));
        return entity;
    }

    /** 使用应用真实YAML绑定，验证目录按前端部门筛选并保留租户过滤。 */
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
        dto.setOrgId(777L);
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
