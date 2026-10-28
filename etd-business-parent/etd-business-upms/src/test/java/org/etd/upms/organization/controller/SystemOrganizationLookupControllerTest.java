package org.etd.upms.organization.controller;

import com.etd.framework.starter.client.core.permission.PermissionChecker;
import jakarta.servlet.Filter;
import org.etd.upms.organization.biz.SystemOrganizationBizService;
import org.etd.upms.organization.controller.vo.SystemOrganizationVO;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.etd.upms.role.service.SystemRoleOrganizationService;
import org.etd.upms.user.service.SystemUserOrganizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证部门回显跨菜单可用，同时管理接口继续执行真实方法权限校验。 */
class SystemOrganizationLookupControllerTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    /** 仅装配 MVC、安全链和真实 Controller，隔离数据库等外部依赖。 */
    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        MockServletContext servletContext = new MockServletContext();
        context.setServletContext(servletContext);
        context.register(TestConfiguration.class);
        context.refresh();
        Filter securityFilter = context.getBean("springSecurityFilterChain", Filter.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
    }

    /** 释放上下文，防止认证状态影响其他测试。 */
    @AfterEach
    void tearDown() {
        context.close();
        SecurityContextHolder.clearContext();
    }

    /** 没有任何菜单权限的登录用户也可按 ID 获取原详情结构。 */
    @Test
    void shouldAllowLookupWithoutMenuPermission() throws Exception {
        SystemOrganizationService service = context.getBean(SystemOrganizationService.class);
        SystemOrganizationVO organization = new SystemOrganizationVO();
        organization.setId(1L);
        organization.setOrgName("研发部");
        when(service.selectById(1L)).thenReturn(organization);
        MockHttpSession session = createAuthenticatedSession();
        MockHttpServletRequestBuilder request = get("/v1/organization/lookup/1").session(session);

        mvc.perform(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orgName").value("研发部"));
        verify(service).selectById(1L);
    }

    /** 回显接口不加入匿名白名单，未登录请求不能进入查询服务。 */
    @Test
    void shouldRejectAnonymousLookup() throws Exception {
        MockHttpServletRequestBuilder request = get("/v1/organization/lookup/1");
        mvc.perform(request).andExpect(status().isUnauthorized());
        verifyNoInteractions(context.getBean(SystemOrganizationService.class));
    }

    /** 原组织管理详情仍要求组织菜单权限，避免放宽整个 Controller。 */
    @Test
    void shouldKeepManagementDetailProtected() throws Exception {
        MockHttpSession session = createAuthenticatedSession();
        MockHttpServletRequestBuilder request = get("/v1/organization/1").session(session);
        mvc.perform(request).andExpect(status().isForbidden());
        verifyNoInteractions(context.getBean(SystemOrganizationService.class));
    }

    /** 构造没有菜单授权的普通登录会话。 */
    private MockHttpSession createAuthenticatedSession() {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated("test-user", null, List.of());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    /** 使用真实框架权限检查器和 Spring Security 方法授权。 */
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfiguration {

        /** 保持业务请求必须登录的安全边界。 */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint((request, response, exception) -> response.setStatus(401)))
                    .build();
        }

        /** 管理接口按生产使用的权限规则检查授权。 */
        @Bean
        PermissionChecker permissionChecker() {
            return new PermissionChecker();
        }

        /** 查询能力替身用于确认请求是否真正进入查询。 */
        @Bean
        SystemOrganizationService organizationService() {
            return mock(SystemOrganizationService.class);
        }

        /** 满足管理 Controller 的编排依赖，不执行写流程。 */
        @Bean
        SystemOrganizationBizService organizationBizService() {
            return mock(SystemOrganizationBizService.class);
        }

        /** 隔离组织编排层自动注入的用户关系依赖。 */
        @Bean
        SystemUserOrganizationService userOrganizationService() {
            return mock(SystemUserOrganizationService.class);
        }

        /** 隔离组织编排层自动注入的角色关系依赖。 */
        @Bean
        SystemRoleOrganizationService roleOrganizationService() {
            return mock(SystemRoleOrganizationService.class);
        }

        /** 原管理入口参与权限回归验证。 */
        @Bean
        SystemOrganizationController organizationController() {
            return new SystemOrganizationController();
        }

        /** 回显入口与管理入口同时注册，验证路由无冲突。 */
        @Bean
        SystemOrganizationLookupController organizationLookupController(SystemOrganizationService service) {
            return new SystemOrganizationLookupController(service);
        }
    }
}
