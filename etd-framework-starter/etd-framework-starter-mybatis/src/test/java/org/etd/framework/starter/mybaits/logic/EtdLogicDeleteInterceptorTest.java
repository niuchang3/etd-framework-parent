package org.etd.framework.starter.mybaits.logic;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.RowBounds;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.MybatisConfig;
import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 验证逻辑删除过滤使用实体元数据，并独立于数据权限忽略项执行。 */
class EtdLogicDeleteInterceptorTest {
    /** 无请求状态的生产SELECT拦截器。 */
    private final EtdLogicDeleteInterceptor interceptor = new EtdLogicDeleteInterceptor();

    /** 注册实际@TableLogic与无逻辑删除字段的两种实体。 */
    @BeforeAll
    static void registerEntities() {
        var builder = new MapperBuilderAssistant(new MybatisConfiguration(), "logic-delete-test");
        builder.setCurrentNamespace("test.LogicMapper");
        TableInfoHelper.initTableInfo(builder, LiveEntity.class);
        TableInfoHelper.initTableInfo(builder, PlainEntity.class);
    }

    /** 清除测试设置的拦截器忽略上下文。 */
    @AfterEach
    void clearIgnoreContext() {
        InterceptorIgnoreHelper.clearIgnoreStrategy();
    }

    /** 普通查询、JOIN、子查询及CTE中的真实表引用均应过滤。 */
    @ParameterizedTest
    @ValueSource(strings = {
        "select * from logic_doc d",
        "select * from logic_doc d join logic_plain p on p.id=d.id",
        "select * from logic_plain p where exists (select 1 from logic_doc d where d.id=p.id)",
        "with visible as (select * from logic_doc d) select * from visible",
        "select id from logic_doc d union all select id from logic_doc x"
    })
    void shouldFilterOnlyMappedLogicalTables(String query) {
        String sql = interceptor.parserSingle(query, "test.Mapper.select");
        assertTrue(sql.contains("d.del_flag = 0"), sql);
        assertFalse(sql.contains("p.del_flag"), sql);
        assertFalse(sql.contains("visible.del_flag"), sql);
        if (query.contains("logic_doc x")) {
            assertTrue(sql.contains("x.del_flag = 0"), sql);
        }
    }

    /** 未声明逻辑删除的实体和未知表不能被猜测追加del_flag。 */
    @Test
    void shouldLeaveTablesWithoutLogicMetadataUnchanged() {
        assertFalse(interceptor.parserSingle("select * from logic_plain", "test.Mapper.select").contains("del_flag"));
        assertFalse(interceptor.parserSingle("select * from unknown_table", "test.Mapper.select").contains("del_flag"));
    }

    /** 内置生成的逻辑删除条件已作为必需AND条件存在时不重复追加。 */
    @ParameterizedTest
    @ValueSource(strings = {
        "select * from logic_doc where del_flag=0 and id=1",
        "select * from logic_doc d where d.del_flag=0 and d.id=1",
        "select * from logic_doc d where (d.del_flag=0 and d.id=1)"
    })
    void shouldNotDuplicateRequiredExistingCondition(String query) {
        String sql = interceptor.parserSingle(query, "test.Mapper.select");
        assertEquals(1, countOccurrences(sql, "del_flag"), sql);
    }

    /** OR中包含未删除条件不足以保证安全，必须在外层重新加AND。 */
    @Test
    void shouldNotTrustLogicConditionInsideOrBranch() {
        String sql = interceptor.parserSingle("select * from logic_doc d where d.del_flag=0 or d.id=1",
                "test.Mapper.select");
        assertEquals(2, countOccurrences(sql, "del_flag"), sql);
        assertTrue(sql.contains(") AND d.del_flag = 0"), sql);
    }

    /** LEFT JOIN过滤右表记录而不把左关联改为内关联。 */
    @Test
    void shouldPreserveLeftJoinSemantics() {
        String sql = interceptor.parserSingle(
                "select p.* from logic_plain p left join logic_doc d on d.id=p.id", "test.Mapper.select");
        assertTrue(sql.contains("ON d.id = p.id AND d.del_flag = 0"), sql);
        assertFalse(sql.contains("WHERE"), sql);
    }

    /** 跳过组织或租户过滤不应导致已删除数据重新可见。 */
    @Test
    void shouldRemainActiveWhenOtherFiltersAreIgnored() {
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().dataPermission(true).tenantLine(true).build());
        assertTrue(rewriteQuery().contains("del_flag = 0"));
    }

    /** 只有显式指定独立逻辑删除忽略项的查询才能读取已删除数据。 */
    @Test
    void shouldHonorExplicitLogicDeleteIgnore() {
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder()
                .others(Map.of(EtdLogicDeleteInterceptor.IGNORE_KEY, true)).build());
        assertFalse(rewriteQuery().contains("del_flag"));
    }

    /** 应用必须显式开启，避免本次扩展改变其他业务模块的自定义查询。 */
    @Test
    void shouldRequireApplicationOptIn() {
        var properties = new EtdMyBatisPlusProperties();
        assertFalse(new MybatisConfig(properties).mybatisPlusInterceptor().getInterceptors()
                .stream().anyMatch(EtdLogicDeleteInterceptor.class::isInstance));
        properties.getLogicDelete().setEnabled(true);
        assertTrue(new MybatisConfig(properties).mybatisPlusInterceptor().getInterceptors()
                .stream().anyMatch(EtdLogicDeleteInterceptor.class::isInstance));
    }

    /** 调用真实beforeQuery，验证忽略开关及BoundSql回写，而非仅测试解析器入口。 */
    private String rewriteQuery() {
        var configuration = new MybatisConfiguration();
        var source = new StaticSqlSource(configuration, "select * from logic_doc d");
        var statement = new MappedStatement.Builder(configuration, "test.Mapper.select", source,
                SqlCommandType.SELECT).build();
        var boundSql = statement.getBoundSql(null);
        interceptor.beforeQuery(null, statement, null, RowBounds.DEFAULT, null, boundSql);
        return boundSql.getSql();
    }

    /** 统计生成SQL中的字段出现次数，用于检测重复条件。 */
    private int countOccurrences(String value, String token) {
        return (value.length() - value.replace(token, "").length()) / token.length();
    }

    /** 继承框架BaseEntity的表，真实继承@TableLogic声明。 */
    @TableName("logic_doc")
    private static class LiveEntity extends BaseEntity {}

    /** 没有逻辑删除字段的表。 */
    @TableName("logic_plain")
    private static class PlainEntity {
        /** 普通业务主键。 */
        @TableId
        private Long id;
    }
}
