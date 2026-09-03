package org.etd.framework.starter.mybaits.permission;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Table;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.DataPermissionHelper;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.context.DataPermissionContextHolder;
import org.etd.framework.starter.mybaits.permission.handler.EtdDataPermissionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EtdDataPermissionHandlerTest {

    private EtdDataPermissionHandler permissionHandler;
    private EtdMyBatisPlusProperties.DataPermissionProperties properties;

    @BeforeEach
    void setUp() {
        RequestContext.clean();
        DataPermissionContextHolder.clear();

        EtdMyBatisPlusProperties rootProperties = new EtdMyBatisPlusProperties();
        properties = rootProperties.getDataPermission();
        properties.setEnabled(true);
        properties.setDefaultOrgColumn("org_id");
        properties.setDefaultUserColumn("create_by");

        permissionHandler = new EtdDataPermissionHandler(properties);
    }

    @AfterEach
    void tearDown() {
        RequestContext.clean();
        DataPermissionContextHolder.clear();
    }

    @Test
    @DisplayName("测试当配置未启用时返回 null")
    void testDisabledConfig() {
        properties.setEnabled(false);
        Expression sqlSegment = permissionHandler.getSqlSegment(businessTable(), null, "com.example.Mapper.select");
        assertNull(sqlSegment);
    }

    @Test
    @DisplayName("测试管理员账户跳过数据权限过滤")
    void testAdminUserSkip() {
        UserDetails adminUser = new UserDetails();
        adminUser.setId(1L);
        adminUser.setPlatformAdmin(true);
        RequestContext.setUser(adminUser);

        Expression sqlSegment = permissionHandler.getSqlSegment(businessTable(), null, "com.example.Mapper.select");
        assertNull(sqlSegment);
    }

    @Test
    @DisplayName("测试全部数据权限范围类型返回 null")
    void testAllPermissionType() {
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.ALL.getCode()));
        RequestContext.setUser(user);

        Expression sqlSegment = permissionHandler.getSqlSegment(businessTable(), null, "com.example.Mapper.select");
        assertNull(sqlSegment);
    }

    @Test
    @DisplayName("测试仅本人数据权限类型生成 SQL Expression")
    void testSelfPermissionType() {
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode()));
        RequestContext.setUser(user);

        Expression sqlSegment = permissionHandler.getSqlSegment(businessTable(), null, "com.example.Mapper.select");
        assertNotNull(sqlSegment);
        assertEquals("biz_order.create_by = 100", sqlSegment.toString());
    }

    @Test
    @DisplayName("测试仅组织数据权限类型生成 SQL Expression")
    void testOrgPermissionType() {
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        user.setScopeOrgIds(Set.of(10L));
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode()));
        RequestContext.setUser(user);

        Expression sqlSegment = permissionHandler.getSqlSegment(businessTable(), null, "com.example.Mapper.select");
        assertNotNull(sqlSegment);
        assertEquals("biz_order.org_id = 10", sqlSegment.toString());
    }

    @Test
    @DisplayName("测试组织及下级组织/自定义多组织生成 IN 表达式")
    void testScopeOrgIdsPermissionType() {
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        user.setScopeOrgIds(Set.of(10L, 20L, 30L));
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.ORGANIZATION_AND_SUBORDINATE.getCode()));
        RequestContext.setUser(user);

        Expression sqlSegment = permissionHandler.getSqlSegment(businessTable(), null, "com.example.Mapper.select");
        assertNotNull(sqlSegment);
        String sql = sqlSegment.toString();
        assertTrue(sql.startsWith("biz_order.org_id IN (") || sql.startsWith("biz_order.org_id IN("),
                "实际生成 SQL: " + sql);
    }

    @Test
    @DisplayName("测试 DataPermissionHelper 忽略标志")
    void testDataPermissionHelperIgnore() {
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode()));
        RequestContext.setUser(user);

        try (DataPermissionHelper.Scope ignore = DataPermissionHelper.ignore()) {
            Expression sqlSegment = permissionHandler.getSqlSegment(
                    businessTable(), null, "com.example.Mapper.select");
            assertNull(sqlSegment);
        }

        Expression sqlSegmentAfter = permissionHandler.getSqlSegment(
                businessTable(), null, "com.example.Mapper.select");
        assertNotNull(sqlSegmentAfter);
    }

    @Test
    @DisplayName("测试注解自定义表别名与列名")
    void testAnnotationCustomAliasAndColumn() {
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        user.setScopeOrgIds(Set.of(10L));
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode()));
        RequestContext.setUser(user);

        DataPermission mockAnnotation = new DataPermission() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return DataPermission.class;
            }

            @Override
            public boolean enable() {
                return true;
            }

            @Override
            public String alias() {
                return "t";
            }

            @Override
            public String orgColumn() {
                return "dept_id";
            }

            @Override
            public String userColumn() {
                return "creator_id";
            }
        };

        DataPermissionContextHolder.push(mockAnnotation);
        try {
            Table table = businessTable();
            table.setAlias(new Alias("t"));
            Expression sqlSegment = permissionHandler.getSqlSegment(table, null, "com.example.Mapper.select");
            assertNotNull(sqlSegment);
            assertEquals("t.dept_id = 10", sqlSegment.toString());
        } finally {
            DataPermissionContextHolder.pop();
        }
    }

    @Test
    @DisplayName("测试保留原始 WHERE 并追加数据权限条件")
    void testPreserveOriginalWhere() {
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode()));
        RequestContext.setUser(user);
        DataPermissionInterceptor interceptor = new DataPermissionInterceptor(permissionHandler);

        String sql = interceptor.parserSingle(
                "select * from biz_order where id = 7", "com.example.Mapper.selectById");

        assertTrue(sql.contains("id = 7"), "实际生成 SQL: " + sql);
        assertTrue(sql.contains("biz_order.create_by = 100"), "实际生成 SQL: " + sql);
        assertTrue(sql.toUpperCase().contains(" AND "), "实际生成 SQL: " + sql);
    }

    @Test
    @DisplayName("测试更新语句保留主键条件")
    void testUpdatePreservesPrimaryKeyCondition() {
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode()));
        RequestContext.setUser(user);
        DataPermissionInterceptor interceptor = new DataPermissionInterceptor(permissionHandler);

        String sql = interceptor.parserMulti(
                "update biz_order set order_name = 'updated' where id = 7", "com.example.Mapper.updateById");

        assertTrue(sql.contains("id = 7"), "实际生成 SQL: " + sql);
        assertTrue(sql.contains("biz_order.create_by = 100"), "实际生成 SQL: " + sql);
        assertTrue(sql.toUpperCase().contains(" AND "), "实际生成 SQL: " + sql);
    }

    @Test
    @DisplayName("测试忽略表不追加数据权限条件")
    void testIgnoreTable() {
        properties.getIgnoreTables().add("sys_role");
        permissionHandler = new EtdDataPermissionHandler(properties);

        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode()));
        RequestContext.setUser(user);

        Expression sqlSegment = permissionHandler.getSqlSegment(
                new Table("sys_role"), null, "com.example.RoleMapper.select");

        assertNull(sqlSegment);
    }

    @Test
    @DisplayName("测试多角色本人及组织权限按并集合并")
    void testMergeMultipleRolePermissionTypes() {
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setPermissionTypes(Set.of(
                BasicConstant.PermissionType.SELF.getCode(),
                BasicConstant.PermissionType.CUSTOM_ORGANIZATION.getCode()));
        user.setScopeOrgIds(Set.of(10L, 20L));
        RequestContext.setUser(user);

        Expression sqlSegment = permissionHandler.getSqlSegment(
                businessTable(), null, "com.example.Mapper.select");

        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.toString().contains("biz_order.create_by = 100"));
        assertTrue(sqlSegment.toString().contains("biz_order.org_id IN"));
        assertTrue(sqlSegment.toString().contains(" OR "));
        DataPermissionInterceptor interceptor = new DataPermissionInterceptor(permissionHandler);
        String sql = interceptor.parserSingle(
                "select * from biz_order where data_status = 1", "com.example.Mapper.select");
        assertTrue(sql.contains("data_status = 1 AND ("), "实际生成 SQL: " + sql);
    }

    @Test
    @DisplayName("测试缺少权限上下文时默认拒绝访问")
    void testMissingPermissionContextFailsClosed() {
        UserDetails user = new UserDetails();
        user.setId(100L);
        RequestContext.setUser(user);

        Expression sqlSegment = permissionHandler.getSqlSegment(
                businessTable(), null, "com.example.Mapper.select");

        assertEquals("1 = 0", sqlSegment.toString());
    }

    @Test
    @DisplayName("测试 data-permission 配置可正确绑定")
    void testDataPermissionConfigurationBinding() {
        MapConfigurationPropertySource propertySource = new MapConfigurationPropertySource(
                Map.of("etd.mybatis.data-permission.enabled", true));

        EtdMyBatisPlusProperties boundProperties = new Binder(propertySource)
                .bind("etd.mybatis", Bindable.of(EtdMyBatisPlusProperties.class))
                .orElseThrow(() -> new IllegalStateException("数据权限配置绑定失败"));

        assertTrue(boundProperties.getDataPermission().getEnabled());
        assertTrue(boundProperties.getDataPermission().getIgnoreTables().isEmpty());
        assertTrue(boundProperties.getTenant().getIgnoreTables().isEmpty());
    }

    private Table businessTable() {
        return new Table("biz_order");
    }
}
