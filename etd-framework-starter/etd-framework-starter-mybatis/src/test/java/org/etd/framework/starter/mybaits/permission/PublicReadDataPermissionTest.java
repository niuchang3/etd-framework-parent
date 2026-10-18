package org.etd.framework.starter.mybaits.permission;

import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.MybatisConfig;
import org.etd.framework.starter.mybaits.permission.aspect.DataPermissionAspect;
import org.etd.framework.starter.mybaits.permission.handler.EtdDataPermissionInterceptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.etd.framework.starter.mybaits.tenant.EtdTenantLineHandler;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import org.etd.framework.starter.mybaits.permission.context.DataPermissionContextHolder;
import org.etd.framework.starter.mybaits.permission.handler.EtdDataPermissionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 通过真实SQL解析链验证多表公开读取、自动别名及严格写权限。 */
class PublicReadDataPermissionTest {
    /** 与生产注册相同的拦截器。 */
    private EtdDataPermissionInterceptor interceptor;

    /** 准备具有本人及部门范围的可信用户。 */
    @BeforeEach
    void setUp() {
        RequestContext.clean();
        DataPermissionContextHolder.clear();
        var properties = new EtdMyBatisPlusProperties();
        properties.getDataPermission().setEnabled(true);
        interceptor = new EtdDataPermissionInterceptor(new EtdDataPermissionHandler(properties.getDataPermission()));
        var user = new UserDetails();
        user.setId(100L);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode(),
                BasicConstant.PermissionType.ORGANIZATION.getCode()));
        user.setScopeOrgIds(Set.of(10L));
        RequestContext.setUser(user);
    }

    /** 防止测试线程的权限声明或登录状态泄漏到后续用例。 */
    @AfterEach
    void clearContext() {
        DataPermissionContextHolder.clear();
        RequestContext.clean();
    }

    /** 别名来自SQL，未声明公开字段的关联表不应被错误追加公开列。 */
    @ParameterizedTest
    @ValueSource(strings = {"d", "doc", "document_alias"})
    void shouldResolveAliasByTable(String alias) throws Exception {
        useRule("documents");
        String sql = select("select " + alias + ".* from docs " + alias
                + " join users u on u.id=" + alias + ".create_by where " + alias + ".tenant_id=7");
        assertTrue(sql.contains(alias + ".is_public = true"), sql);
        assertTrue(sql.contains(alias + ".create_by = 100"), sql);
        assertTrue(sql.contains(alias + ".org_id = 10"), sql);
        assertTrue(sql.contains(alias + ".tenant_id = 7 AND ("), sql);
        assertFalse(sql.contains("u.is_public"), sql);
        assertFalse(sql.contains("u.org_id"), sql);
    }

    /** 无别名时使用真实表名，带schema的声明只匹配对应schema。 */
    @Test
    void shouldResolveTableWithoutAliasAndSchema() throws Exception {
        useRule("documents");
        assertTrue(select("select * from docs").contains("docs.is_public = true"));
        useRule("schemaDocuments");
        String sql = select("select d.* from public.docs d join other.docs x on x.id=d.id");
        assertTrue(sql.contains("d.is_public = true"), sql);
        assertFalse(sql.contains("x.is_public"), sql);
    }

    /** 两个表分别配置公开列，LEFT JOIN右侧的过滤保留在ON中。 */
    @Test
    void shouldApplyMultipleRulesToJoin() throws Exception {
        useRule("documentsAndFolders");
        String sql = select("select d.* from docs d left join folders f on f.id=d.folder_id where d.tenant_id=7");
        assertTrue(sql.contains("d.is_public = true"), sql);
        assertTrue(sql.contains("f.published = true"), sql);
        assertTrue(sql.indexOf("f.published") < sql.indexOf("WHERE"), sql);
        assertTrue(sql.contains("f.owner_id = 100"), sql);
    }

    /** 同表多次引用默认全部过滤，也允许显式别名只选择其中一个引用。 */
    @Test
    void shouldSupportSelfJoinAndExplicitAlias() throws Exception {
        String query = "select d.id from docs d join docs parent on parent.id=d.parent_id";
        useRule("documents");
        String sql = select(query);
        assertTrue(sql.contains("parent.is_public = true"), sql);
        useRule("oneAlias");
        sql = select(query);
        assertTrue(sql.contains("d.is_public = true"), sql);
        assertFalse(sql.contains("parent.is_public"), sql);
    }

    /** EXISTS、UNION、CTE内的真实表引用也需要在解析过程中获得权限条件。 */
    @ParameterizedTest
    @ValueSource(strings = {
        "select * from users u where exists (select 1 from docs d where d.create_by=u.id)",
        "select id from docs d union all select id from docs d2",
        "with visible as (select id from docs d) select * from visible",
        "select * from (select id from docs d) t",
        "with recursive paths as (select id,parent_id from docs d where parent_id is null "
            + "union all select p.id,p.parent_id from docs p join paths c on p.parent_id=c.id) select * from paths"
    })
    void shouldFilterNestedQueries(String query) throws Exception {
        useRule("documents");
        String sql = select(query);
        assertTrue(sql.contains("d.is_public = true"), sql);
        if (query.contains("d2")) {
            assertTrue(sql.contains("d2.is_public = true"), sql);
        }
        assertFalse(sql.contains("visible.org_id"), sql);
    }

    /** 无角色范围的已登录用户仍可读公开资料；匿名用户不能借公开规则进入。 */
    @Test
    void shouldRequireLoginButAllowPublicWithoutRoleScope() throws Exception {
        useRule("documents");
        RequestContext.getUser().setPermissionTypes(Set.of());
        assertTrue(select("select * from docs d").contains("d.is_public = true"));
        RequestContext.clean();
        String sql = select("select * from docs d");
        assertTrue(sql.contains("1 = 0"), sql);
        assertFalse(sql.contains("is_public"), sql);
    }

    /** 公开资料不授予编辑或删除权限，无角色范围时写入条件必须恒假。 */
    @ParameterizedTest
    @ValueSource(strings = {
        "update docs d set title='new' where d.id=1 or d.id=2",
        "delete from docs d where d.id=1 or d.id=2"
    })
    void shouldKeepWritesStrict(String query) throws Exception {
        useRule("documents");
        String sql = interceptor.parserMulti(query, "example.Mapper.write");
        assertFalse(sql.contains("is_public"), sql);
        assertTrue(sql.contains("d.create_by = 100"), sql);
        assertTrue(sql.contains("(d.id = 1 OR d.id = 2) AND"), sql);
        RequestContext.getUser().setPermissionTypes(Set.of());
        assertTrue(interceptor.parserMulti(query, "example.Mapper.write").contains("1 = 0"));
        // 写入解析后再次查询仍可使用公开规则，不能残留操作类型状态。
        assertTrue(select("select * from docs d").contains("d.is_public = true"));
    }

    /** 没有公开声明的表继续使用原有角色范围，拥有全部范围时不额外限制。 */
    @Test
    void shouldPreserveOrdinaryAndAllPermissions() throws Exception {
        useRule("ordinary");
        String sql = select("select * from docs d");
        assertFalse(sql.contains("is_public"), sql);
        assertTrue(sql.contains("d.create_by = 100"), sql);
        useRule("documents");
        RequestContext.getUser().setPermissionTypes(Set.of(BasicConstant.PermissionType.ALL.getCode()));
        assertFalse(select("select * from docs d").contains("WHERE"));
    }

    /** 重叠声明和SQL表达式字段必须报错，禁止静默选择更宽松规则。 */
    @Test
    void shouldRejectAmbiguousOrInvalidRules() throws Exception {
        useRule("overlapping");
        assertThrows(RuntimeException.class, () -> select("select * from docs d"));
        useRule("invalid");
        assertThrows(RuntimeException.class, () -> select("select * from docs d"));
    }

    /** 租户拦截先执行后，公开条件仍必须受租户条件约束。 */
    @Test
    void shouldPreserveTenantIsolation() throws Exception {
        useRule("documents");
        RequestContext.setTenantCode(7L);
        var tenant = new TenantLineInnerInterceptor(new EtdTenantLineHandler("tenant_id"));
        String sql = select(tenant.parserSingle("select * from docs d", "example.Mapper.select"));
        assertTrue(sql.contains("d.tenant_id = 7 AND ("), sql);
        assertTrue(sql.contains("d.is_public = true"), sql);
    }

    /** 通过真实Spring切面传播查询规则，执行结束后必须恢复外层上下文。 */
    @Test
    void shouldApplyRulesThroughSpringAspect() {
        var factory = new AspectJProxyFactory(new QueryService(interceptor));
        factory.addAspect(new DataPermissionAspect());
        QueryService proxy = factory.getProxy();
        assertTrue(proxy.fetchDocuments().contains("d.is_public = true"));
        assertNull(DataPermissionContextHolder.peek());
        assertThrows(IllegalStateException.class, proxy::failQuery);
        assertNull(DataPermissionContextHolder.peek());
    }

    /** 生产自动配置必须实际注册支持公开读取的新拦截器。 */
    @Test
    void shouldRegisterPublicReadInterceptor() {
        var properties = new EtdMyBatisPlusProperties();
        properties.getDataPermission().setEnabled(true);
        var chain = new MybatisConfig(properties).mybatisPlusInterceptor();
        assertTrue(chain.getInterceptors().stream().anyMatch(EtdDataPermissionInterceptor.class::isInstance));
    }

    /** 使用真实注解元数据模拟切面建立的作用域，不依赖手写注解实现。 */
    private void useRule(String method) throws Exception {
        DataPermissionContextHolder.clear();
        DataPermissionContextHolder.push(Rules.class.getDeclaredMethod(method).getAnnotation(DataPermission.class));
    }

    /** 与生产SELECT解析入口使用相同的Mapper标识参数。 */
    private String select(String sql) {
        return interceptor.parserSingle(sql, "example.Mapper.select");
    }

    /** 用真实代理验证业务服务上的声明，不在测试中代替切面压入规则。 */
    public static class QueryService {
        /** 查询使用的生产权限拦截器。 */
        private final EtdDataPermissionInterceptor interceptor;

        /** @param interceptor 与生产相同的SQL权限解析器 */
        public QueryService(EtdDataPermissionInterceptor interceptor) {
            this.interceptor = interceptor;
        }

        /** @return 注入公开读取规则后的查询SQL */
        @DataPermission(tables = @DataPermissionTable(table = "docs", publicColumn = "is_public"))
        public String fetchDocuments() {
            return interceptor.parserSingle("select * from docs d", "example.Mapper.select");
        }

        /** 验证业务异常不会泄漏权限上下文。 */
        @DataPermission(tables = @DataPermissionTable(table = "docs", publicColumn = "is_public"))
        public void failQuery() {
            throw new IllegalStateException("模拟查询失败");
        }
    }

    /** 各种业务查询的显式表级权限声明。 */
    private static class Rules {
        /** 只过滤文档表，自动采用SQL别名。 */
        @DataPermission(tables = @DataPermissionTable(table = "docs", publicColumn = "is_public"))
        void documents() {}

        /** 两张表的公开字段和创建人字段可分别声明。 */
        @DataPermission(tables = {
            @DataPermissionTable(table = "docs", publicColumn = "is_public"),
            @DataPermissionTable(table = "folders", publicColumn = "published", userColumn = "owner_id")
        })
        void documentsAndFolders() {}

        /** schema明确时不匹配同名的其他schema表。 */
        @DataPermission(tables = @DataPermissionTable(table = "public.docs", publicColumn = "is_public"))
        void schemaDocuments() {}

        /** 自关联中的一个角色可显式指定别名。 */
        @DataPermission(tables = @DataPermissionTable(table = "docs", alias = "d", publicColumn = "is_public"))
        void oneAlias() {}

        /** 表没有公开字段时继续使用普通数据范围。 */
        @DataPermission(tables = @DataPermissionTable(table = "docs"))
        void ordinary() {}

        /** 同一引用不能同时命中通用规则和别名规则。 */
        @DataPermission(tables = {
            @DataPermissionTable(table = "docs", publicColumn = "is_public"),
            @DataPermissionTable(table = "docs", alias = "d", publicColumn = "is_public")
        })
        void overlapping() {}

        /** 字段声明不允许包含SQL表达式。 */
        @DataPermission(tables = @DataPermissionTable(table = "docs", publicColumn = "is_public OR 1=1"))
        void invalid() {}
    }
}
