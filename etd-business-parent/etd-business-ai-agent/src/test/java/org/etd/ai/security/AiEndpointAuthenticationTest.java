package org.etd.ai.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.etd.framework.starter.client.autoconfigure.SecurityClientAutoConfiguration;
import com.etd.framework.starter.client.core.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.client.core.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.client.core.authentication.EtdAuthenticationFailureHandler;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.i18n.SecurityMessageResolver;
import com.etd.framework.starter.client.core.permission.PermissionChecker;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.storage.UserLoginTokenStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;

import jakarta.servlet.Filter;

import org.etd.ai.knowledge.controller.KnowledgeFolderController;
import org.etd.ai.model.biz.ModelBizService;
import org.etd.ai.model.biz.ModelSquareBizService;
import org.etd.ai.model.controller.ModelController;
import org.etd.ai.model.controller.ModelSquareController;
import org.etd.ai.model.service.ModelSquareService;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.springframework.http.MediaType;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 验证AI直接声明框架权限后，登录、功能read/write与公开健康路径的访问边界。 */
class AiEndpointAuthenticationTest {
    /** 仅装配真实安全过滤链、MVC及模型页面，外部认证存储使用替身。 */
    private AnnotationConfigWebApplicationContext context;

    /** 经过真实安全过滤链的HTTP测试入口。 */
    private MockMvc mvc;

    /** 创建隔离上下文，不连接数据库、Redis或模型服务。 */
    @BeforeEach
    void createSecurityContext() {
        context = new AnnotationConfigWebApplicationContext();
        var servletContext = new MockServletContext();
        context.setServletContext(servletContext);
        context.register(AuthenticationConfiguration.class);
        context.refresh();
        Filter securityFilter = context.getBean("springSecurityFilterChain", Filter.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
    }

    /** 关闭过滤链上下文并清除线程认证，避免用例相互污染。 */
    @AfterEach
    void closeSecurityContext() {
        context.close();
        SecurityContextHolder.clearContext();
        RequestContext.clean();
    }

    /** 匿名业务请求仍被框架认证入口拒绝。 */
    @Test
    void shouldRequireAuthenticationForBusinessEndpoints() throws Exception {
        var request = get("/v1/model-square/connections");
        mvc.perform(request).andExpect(status().isUnauthorized());
        var writeRequest = delete("/v1/model-square/connections/1");
        mvc.perform(writeRequest).andExpect(status().isUnauthorized());
    }

    /** 登录但无功能权限时，读写接口均拒绝且不能进入业务服务。 */
    @Test
    void shouldDenyAuthenticatedUserWithoutFunctionalAuthorities() throws Exception {
        var session = createSession();
        var readRequest = get("/v1/model-square/connections").session(session);
        mvc.perform(readRequest).andExpect(status().isForbidden());
        var writeRequest = delete("/v1/model-square/connections/1").session(session);
        mvc.perform(writeRequest).andExpect(status().isForbidden());
        var models = context.getBean(ModelConnectionService.class);
        verifyNoInteractions(models);
    }

    /** read只允许查询，不能执行删除等写操作。 */
    @Test
    void shouldAllowReadWithoutGrantingWrite() throws Exception {
        var session = createSession("ai:model-square:read");
        var readRequest = get("/v1/model-square/connections").session(session);
        mvc.perform(readRequest).andExpect(status().isOk());
        var writeRequest = delete("/v1/model-square/connections/1").session(session);
        mvc.perform(writeRequest).andExpect(status().isForbidden());
        var models = context.getBean(ModelConnectionService.class);
        verify(models).fetchPage(1L, 10L, null, null, null);
        verify(models, never()).removeConnection(anyLong());
    }

    /** 同时具备read/write时，真实Controller可调用读写业务能力。 */
    @Test
    void shouldAllowGrantedReadAndWrite() throws Exception {
        var session = createSession("ai:model-square:read", "ai:model-square:write");
        var readRequest = get("/v1/model-square/connections").session(session);
        mvc.perform(readRequest).andExpect(status().isOk());
        var writeRequest = delete("/v1/model-square/connections/1").session(session);
        mvc.perform(writeRequest).andExpect(status().isOk());
        var models = context.getBean(ModelConnectionService.class);
        verify(models).fetchPage(1L, 10L, null, null, null);
        verify(models).removeConnection(1L);
    }

    /** 全局目录要求read权限；普通用户即使有write权限也不能维护。 */
    @Test
    void shouldKeepCatalogReadOnlyForTenantUsers() throws Exception {
        var anonymous = get("/v1/models");
        mvc.perform(anonymous).andExpect(status().isUnauthorized());
        var deniedSession = createSession("ai:model-square:read");
        var denied = get("/v1/models").session(deniedSession);
        mvc.perform(denied).andExpect(status().isForbidden());
        var session = createSession("ai:model:read", "ai:model:write");
        var catalog = get("/v1/models").session(session);
        mvc.perform(catalog).andExpect(status().isOk());
        var write = post("/v1/models").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(MODEL_REQUEST);
        mvc.perform(write).andExpect(status().isForbidden());
        RequestContext.getUser().setTenantAdmin(true);
        mvc.perform(write).andExpect(status().isForbidden());
        var update = put("/v1/models/1").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(MODEL_REQUEST);
        mvc.perform(update).andExpect(status().isForbidden());
        var remove = delete("/v1/models/1").session(session);
        mvc.perform(remove).andExpect(status().isForbidden());
        var removed = get("/v1/model-configs").session(session);
        mvc.perform(removed).andExpect(status().isNotFound());
    }

    /** 平台管理员无需人工写SQL即可维护目录，真实能力服务仍执行身份检查。 */
    @Test
    void shouldAllowPlatformAdministratorToManageCatalog() throws Exception {
        var session = createPlatformSession(false);
        var create = post("/v1/models").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(MODEL_REQUEST);
        mvc.perform(create).andExpect(status().isOk());
        var update = put("/v1/models/1").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(MODEL_REQUEST);
        mvc.perform(update).andExpect(status().isOk());
        var remove = delete("/v1/models/1").session(session);
        mvc.perform(remove).andExpect(status().isOk());
        var mapper = context.getBean(ModelMapper.class);
        verify(mapper).insert(any(ModelEntity.class));
        verify(mapper).update(any(ModelEntity.class), any());
        verify(mapper).deleteById(1L);
    }

    /** 锁定身份不能利用平台管理员豁免绕过写保护。 */
    @Test
    void shouldRejectReadOnlyPlatformAdministrator() throws Exception {
        var session = createPlatformSession(true);
        var write = post("/v1/models").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(MODEL_REQUEST);
        mvc.perform(write).andExpect(status().isForbidden());
        var mapper = context.getBean(ModelMapper.class);
        verifyNoInteractions(mapper);
    }

    /** 有效完整模型请求，避免参数错误掩盖权限边界。 */
    private static final String MODEL_REQUEST = """
            {"providerCode":"aliyun","modelName":"测试模型","modelCode":"test-model",
             "modelType":"chat","sort":0,"dataStatus":1,"version":0}
            """;

    /** 管理身份同时放入会话认证详情与框架可信请求上下文。 */
    private MockHttpSession createPlatformSession(boolean readOnly) {
        var session = createSession();
        var user = RequestContext.getUser();
        user.setPlatformAdmin(true);
        user.setLocked(readOnly);
        return session;
    }

    /** 为请求装配指定authority，避免依赖外部UPMS令牌签发。 */
    private MockHttpSession createSession(String... authorityCodes) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String code : authorityCodes) {
            var authority = new SimpleGrantedAuthority(code);
            authorities.add(authority);
        }
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated("tenant-user", "", authorities);
        var user = new UserDetails();
        user.setId(1L);
        authentication.setDetails(user);
        RequestContext.setUser(user);
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        var session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    /** 广场单一read权限覆盖模型和连接全部查询，不能替代平台模型目录管理权限。 */
    @Test
    void shouldAllowAllSquareReadsWithOneAuthority() throws Exception {
        var paths = List.of("/v1/model-square/models", "/v1/model-square/models/1",
                "/v1/model-square/connections", "/v1/model-square/connections/1");
        var session = createSession("ai:model-square:read");
        for (var path : paths) {
            var request = get(path).session(session);
            mvc.perform(request).andExpect(status().isOk());
        }
        var forbidden = get("/v1/models").session(session);
        mvc.perform(forbidden).andExpect(status().isForbidden());
        var mapper = context.getBean(ModelMapper.class);
        verify(mapper, never()).insert(any(ModelEntity.class));
    }

    /** 广场read只允许浏览，广场write可接入具体模型，无需第二个菜单资源权限。 */
    @Test
    void shouldConnectModelWithSquareWriteAuthority() throws Exception {
        var payload = """
                {"modelId":"1","protocol":"ollama","baseUrl":"http://127.0.0.1:11434","authType":"none"}
                """;
        var browseOnly = createSession("ai:model-square:read");
        var denied = post("/v1/model-square/connections").session(browseOnly)
                .contentType(MediaType.APPLICATION_JSON).content(payload);
        mvc.perform(denied).andExpect(status().isForbidden());
        var writable = createSession("ai:model-square:read", "ai:model-square:write");
        var allowed = post("/v1/model-square/connections").session(writable)
                .contentType(MediaType.APPLICATION_JSON).content(payload);
        mvc.perform(allowed).andExpect(status().isOk());
        var connections = context.getBean(ModelConnectionService.class);
        verify(connections).createConnection(argThat(connection -> connection.getModelId().equals(1L)), isNull());
        var missingModel = post("/v1/model-square/connections").session(writable)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"protocol\":\"ollama\",\"baseUrl\":\"http://127.0.0.1:11434\",\"authType\":\"none\"}");
        mvc.perform(missingModel).andExpect(status().isBadRequest());
    }

    /** 修改连接也按广场write授权，并将目标连接及版本交给真实广场Biz编排。 */
    @Test
    void shouldUpdateConnectionWithSquareWriteAuthority() throws Exception {
        var payload = """
                {"modelId":"1","protocol":"ollama","baseUrl":"http://127.0.0.1:11434",
                 "authType":"none","version":0}
                """;
        var readOnly = createSession("ai:model-square:read");
        var denied = put("/v1/model-square/connections/1").session(readOnly)
                .contentType(MediaType.APPLICATION_JSON).content(payload);
        mvc.perform(denied).andExpect(status().isForbidden());
        var writable = createSession("ai:model-square:read", "ai:model-square:write");
        var allowed = put("/v1/model-square/connections/1").session(writable)
                .contentType(MediaType.APPLICATION_JSON).content(payload);
        mvc.perform(allowed).andExpect(status().isOk());
        var connections = context.getBean(ModelConnectionService.class);
        verify(connections).modifyConnection(argThat(connection -> connection.getId().equals(1L)), isNull(), eq(0L));
    }

    /** 聚合连接详情保持只返回凭证存在标记，不因移动入口而泄露内部存储字段。 */
    @Test
    void shouldKeepAggregatedConnectionResponseSecretFree() throws Exception {
        var session = createSession("ai:model-square:read");
        var request = get("/v1/model-square/connections/1").session(session);
        var response = mvc.perform(request).andExpect(status().isOk()).andReturn();
        var body = response.getResponse().getContentAsString();
        assertTrue(body.contains("\"credentialConfigured\":true"));
        assertFalse(body.contains("test-encrypted-secret"));
        assertFalse(body.contains("test-key-id"));
        assertFalse(body.contains("apiKeyCiphertext"));
        assertFalse(body.contains("credentialKeyId"));
    }

    /** 移除旧Controller后，已认证用户访问旧路径仍应404，不保留别名或兼容入口。 */
    @Test
    void shouldRemoveStandaloneConnectionEndpoints() throws Exception {
        var session = createPlatformSession(false);
        var paths = List.of("/v1/model-connections", "/v1/model-connections/1",
                "/v1/model-square/providers");
        for (var path : paths) {
            var request = get(path).session(session);
            mvc.perform(request).andExpect(status().isNotFound());
        }
        var write = delete("/v1/model-connections/1").session(session);
        mvc.perform(write).andExpect(status().isNotFound());
    }

    /** 旧连接器资源码不再授权广场，防止移除菜单后形成隐式权限别名。 */
    @Test
    void shouldRejectRemovedConnectionAuthority() throws Exception {
        var session = createSession("ai:model-connection:read", "ai:model-connection:write");
        var read = get("/v1/model-square/connections").session(session);
        mvc.perform(read).andExpect(status().isForbidden());
        var write = delete("/v1/model-square/connections/1").session(session);
        mvc.perform(write).andExpect(status().isForbidden());
        var connections = context.getBean(ModelConnectionService.class);
        verifyNoInteractions(connections);
    }

    /** 广场read/write权限不能绕过用户及平台管理员的只读身份保护。 */
    @Test
    void shouldRejectSquareWritesForReadOnlyIdentity() throws Exception {
        var session = createSession("ai:model-square:read", "ai:model-square:write");
        RequestContext.getUser().setLocked(true);
        var request = delete("/v1/model-square/connections/1").session(session);
        mvc.perform(request).andExpect(status().isForbidden());
        RequestContext.getUser().setPlatformAdmin(true);
        mvc.perform(request).andExpect(status().isForbidden());
        var connections = context.getBean(ModelConnectionService.class);
        verifyNoInteractions(connections);
    }

    /** 连接启停遵循write权限，参数合法后才进入编排服务。 */
    @Test
    void shouldRequireWriteForConnectionStatus() throws Exception {
        var readOnly = createSession("ai:model-square:read");
        var denied = put("/v1/model-square/connections/1/status").session(readOnly)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dataStatus\":0,\"version\":0}");
        mvc.perform(denied).andExpect(status().isForbidden());
        var writable = createSession("ai:model-square:read", "ai:model-square:write");
        var allowed = put("/v1/model-square/connections/1/status").session(writable)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dataStatus\":0,\"version\":0}");
        mvc.perform(allowed).andExpect(status().isOk());
        var invalid = put("/v1/model-square/connections/1/status").session(writable)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dataStatus\":2,\"version\":0}");
        mvc.perform(invalid).andExpect(status().isBadRequest());
    }

    /** 健康路径仍使用框架匿名白名单，未将全部AI路径加入白名单。 */
    @Test
    void shouldKeepHealthPathPublic() throws Exception {
        var request = get("/actuator/health");
        mvc.perform(request).andExpect(status().isOk());
    }

    /** 首页所有聚合接口使用广场资源码，平台模型目录仍保持独立管理权限。 */
    @Test
    void shouldDeclareFrameworkPermissionOnAllAiControllers() {
        var resources =
                Map.of(
                        ModelController.class, "ai:model",
                        ModelSquareController.class, "ai:model-square",
                        KnowledgeFolderController.class, "ai:knowledge-folder");
        for (var resource : resources.entrySet()) {
            var permission =
                    AnnotatedElementUtils.findMergedAnnotation(resource.getKey(), Permission.class);
            assertNotNull(permission);
            assertEquals(resource.getValue(), permission.value());
        }
    }

    /** 仅提供健康响应，用于检查生产过滤链的匿名路径匹配。 */
    @RestController
    static class HealthController {
        /** 返回健康占位响应，不涉及外部基础设施探测。 */
        @GetMapping("/actuator/health")
        Map<String, String> getHealth() {
            return Map.of("status", "UP");
        }
    }

    /** 复用生产过滤链定义，认证依赖隔离，避免测试复制另一套放行策略。 */
    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableMethodSecurity
    @EnableWebMvc
    static class AuthenticationConfiguration {
        /** 使用应用相同的健康白名单和authenticated兜底规则。 */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper json)
                throws Exception {
            var properties = new SecurityProperties();
            var publicPaths = List.of("/actuator/health");
            properties.getPermissions().setIgnore(publicPaths);
            SecurityMessageResolver messages = (code, args, locale, defaultMessage) -> code;
            var denied = new AccessDeniedHandlerImpl(json, messages);
            var unauthenticated = new AuthenticationEntryPointImpl(json, messages);
            var configuration = new SecurityClientAutoConfiguration();
            return configuration.bearerAuthenticationSecurityFilterChain(
                    http, properties, denied, unauthenticated);
        }

        /** 使用生产框架方法授权检查器，不在测试中模拟权限校验结果。 */
        @Bean
        PermissionChecker permissionChecker() {
            return new PermissionChecker();
        }

        /** 本测试通过会话提供已认证身份；若意外触发解码则立即失败。 */
        @Bean
        TokenDecode<SignedJWT> tokenDecode() {
            return token -> {
                throw new IllegalStateException("本用例不应触发外部令牌解码");
            };
        }

        /** 隔离登录存储，测试不依赖Redis。 */
        @Bean
        UserLoginTokenStorage userLoginTokenStorage() {
            return mock(UserLoginTokenStorage.class);
        }

        /** 无效令牌分支的依赖替身，不影响匿名请求的真实401入口。 */
        @Bean
        EtdAuthenticationFailureHandler failureHandler() {
            return mock(EtdAuthenticationFailureHandler.class);
        }

        /** 供真实框架认证链与MVC响应序列化使用。 */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        /** 不访问数据库，仅验证接口是否经过认证后进入服务。 */
        @Bean
        ModelConnectionService modelConnectionService() {
            var models = mock(ModelConnectionService.class);
            var page = new Page<ModelConnectionEntity>(1L, 10L);
            when(models.fetchPage(1L, 10L, null, null, null)).thenReturn(page);
            var connection = new ModelConnectionEntity();
            connection.setId(1L);
            connection.setModelId(1L);
            connection.setApiKeyCiphertext("test-encrypted-secret");
            connection.setCredentialKeyId("test-key-id");
            when(models.fetchConnection(1L)).thenReturn(connection);
            when(models.requireConnection(1L, false)).thenReturn(connection);
            when(models.createConnection(any(), any())).thenReturn(1L);
            return models;
        }

        /** 广场查询使用真实服务和Mapper替身，以验证Controller权限及响应转换。 */
        @Bean
        ModelSquareService modelSquareService(ModelMapper mapper) {
            return new ModelSquareService(mapper);
        }

        /** 广场使用真实Biz组合模型和连接能力，确保安全测试覆盖实际接口调用链。 */
        @Bean
        ModelSquareBizService modelSquareBizService(ModelSquareService queries,
                ModelService models, ModelConnectionService connections) {
            return new ModelSquareBizService(queries, models, connections);
        }

        /** 聚合Controller统一按广场权限参与真实框架方法安全校验。 */
        @Bean
        ModelSquareController modelSquareController(ModelSquareBizService biz) {
            return new ModelSquareController(biz);
        }

        /** 真实目录能力服务使用Mapper替身，身份规则不做mock。 */
        @Bean
        ModelMapper modelMapper() {
            var configuration = new MybatisConfiguration();
            var assistant = new MapperBuilderAssistant(configuration, "catalog-security-test");
            TableInfoHelper.initTableInfo(assistant, ModelEntity.class);
            var mapper = mock(ModelMapper.class);
            var page = new Page<ModelEntity>(1L, 10L);
            when(mapper.selectPage(any(), any())).thenReturn(page);
            when(mapper.selectSquareModelPage(any(), any(), any(), any(), any()))
                    .thenAnswer(call -> call.getArgument(0));
            var model = new ModelEntity();
            model.setId(1L);
            model.setVersion(0L);
            model.setDataStatus(1);
            when(mapper.selectOne(any())).thenReturn(model);
            when(mapper.update(any(ModelEntity.class), any())).thenReturn(1);
            when(mapper.deleteById(1L)).thenReturn(1);
            return mapper;
        }

        /** 目录写规则通过真实能力服务执行。 */
        @Bean
        ModelService modelService(ModelMapper mapper) {
            return new ModelService(mapper);
        }

        /** 关联引用检查使用真实编排逻辑。 */
        @Bean
        ModelBizService modelBizService(ModelService models, ModelConnectionService connections) {
            return new ModelBizService(models, connections);
        }

        /** 使用真实目录Controller参与安全链测试。 */
        @Bean
        ModelController modelController(ModelService models, ModelBizService biz) {
            return new ModelController(models, biz);
        }

        /** 提供白名单匹配的公开端点。 */
        @Bean
        HealthController healthController() {
            return new HealthController();
        }
    }
}
