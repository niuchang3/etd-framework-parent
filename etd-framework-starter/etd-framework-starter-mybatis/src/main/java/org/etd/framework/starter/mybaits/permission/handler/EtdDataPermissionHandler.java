package org.etd.framework.starter.mybaits.permission.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.google.common.collect.Sets;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.BooleanValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import org.etd.framework.starter.mybaits.permission.context.DataPermissionContextHolder;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 框架默认数据权限处理器
 * <p>
 * 基于当前登录用户的权限范围（DataScope/PermissionType）自动生成数据隔离 SQL Expression。
 *
 * @author 牛昌
 */
public class EtdDataPermissionHandler implements MultiDataPermissionHandler {

    /** 全局数据权限开关及默认字段配置。 */
    private final EtdMyBatisPlusProperties.DataPermissionProperties properties;
    /** 由应用显式声明不参与数据权限过滤的表。 */
    private final Set<String> ignoreTables = Sets.newHashSet();

    /**
     * 初始化数据权限处理器。
     * @param properties 数据权限配置；为空时关闭过滤
     */
    public EtdDataPermissionHandler(EtdMyBatisPlusProperties.DataPermissionProperties properties) {
        this.properties = properties;
        if (properties != null && !CollectionUtils.isEmpty(properties.getIgnoreTables())) {
            addIgnoreTables(properties.getIgnoreTables());
        }
    }

    /**
     * 注册无需组织或本人过滤的表，不影响独立的租户拦截器。
     * @param tableNames 后端配置的忽略表名集合
     * @return 当前处理器，便于初始化配置
     */
    public EtdDataPermissionHandler addIgnoreTables(List<String> tableNames) {
        if (tableNames != null) {
            for (String tableName : tableNames) {
                if (StringUtils.hasText(tableName)) {
                    ignoreTables.add(tableName.toUpperCase().trim());
                }
            }
        }
        return this;
    }

    /**
     * 获取数据权限 SQL 片段条件
     *
     * @param table             当前处理的表
     * @param where             原 SQL 的 WHERE 条件，由拦截器负责与返回条件执行 AND 合并
     * @param mappedStatementId Mapper 方法 ID
     * @return 增强的数据权限 SQL 条件 Expression（返回 null 表示无视数据权限）
     */
    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        // 标准处理器入口保持严格数据范围；只有框架SELECT入口允许公开读取。
        return getSqlSegment(table, where, mappedStatementId, false);
    }

    /**
     * 为指定表生成数据权限条件，使用SQL解析得到的真实表名及别名。
     *
     * @param table 当前SQL中的表引用
     * @param where 原查询条件，由拦截器负责AND合并
     * @param mappedStatementId Mapper语句标识
     * @param publicRead 是否为允许公开扩展的SELECT入口，写操作必须为false
     * @return 权限表达式；null表示该表无需追加限制
     */
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId, boolean publicRead) {
        if (!shouldApplyDataPermission() || isIgnoreTable(table.getName())) {
            return null;
        }
        DataPermission annotation = DataPermissionContextHolder.peek();
        if (annotation != null && (!annotation.enable() || !matchesTargetTable(table, annotation.alias()))) {
            return null;
        }
        DataPermissionTable rule = resolveTableRule(table, annotation);
        if (annotation != null && annotation.tables().length > 0 && rule == null) {
            return null;
        }
        UserDetails user = RequestContext.getUser();
        if (user == null) {
            return buildAlwaysFalseExpression();
        }
        DataPermissionScope scope = DataPermissionScope.from(user);
        if (scope.all()) {
            return null;
        }
        return buildAccessExpression(table, scope, annotation, rule, publicRead);
    }

    /** 将可选公开读取与角色范围组成一个括号表达式，避免OR改变原查询条件优先级。 */
    private Expression buildAccessExpression(Table table, DataPermissionScope scope, DataPermission annotation,
            DataPermissionTable rule, boolean publicRead) {
        Expression permission = buildRoleExpression(table, scope, annotation, rule);
        if (publicRead && rule != null && StringUtils.hasText(rule.publicColumn())) {
            // 公开列必须为布尔类型，NULL不会被认作公开。
            Column publicField = buildColumn(table, rule.publicColumn());
            var publicValue = new BooleanValue(true);
            Expression published = new EqualsTo(publicField, publicValue);
            permission = appendOr(published, permission);
        }
        return permission == null ? buildAlwaysFalseExpression() : parenthesizeOr(permission);
    }

    /** 只复用可信登录上下文的角色范围，公开读取不依赖用户拥有本人或部门范围。 */
    private Expression buildRoleExpression(Table table, DataPermissionScope scope, DataPermission annotation,
            DataPermissionTable rule) {
        String orgColumn = resolveColumn(rule == null ? "" : rule.orgColumn(),
                annotation == null ? "" : annotation.orgColumn(), properties.getDefaultOrgColumn());
        String userColumn = resolveColumn(rule == null ? "" : rule.userColumn(),
                annotation == null ? "" : annotation.userColumn(), properties.getDefaultUserColumn());
        Expression permission = null;
        if (scope.self() && scope.userId() != null) {
            Column userField = buildColumn(table, userColumn);
            var userId = new LongValue(scope.userId());
            permission = new EqualsTo(userField, userId);
        }
        if (!scope.orgIds().isEmpty()) {
            Column organizationField = buildColumn(table, orgColumn);
            Expression organizationPermission = buildOrganizationExpression(organizationField, scope.orgIds());
            permission = appendOr(permission, organizationPermission);
        }
        return permission;
    }

    /** 多条规则匹配同一表引用时拒绝执行，避免依赖规则顺序意外改变授权。 */
    private DataPermissionTable resolveTableRule(Table table, DataPermission annotation) {
        DataPermissionTable matched = null;
        if (annotation == null) {
            return null;
        }
        for (DataPermissionTable rule : annotation.tables()) {
            validateTableRule(rule);
            String actual = rule.table().contains(".") ? table.getFullyQualifiedName() : table.getName();
            if (rule.table().equalsIgnoreCase(actual.replace("\"", ""))
                    && matchesTargetTable(table, rule.alias())) {
                if (matched != null) {
                    throw new IllegalArgumentException("同一SQL表引用匹配了多条数据权限规则");
                }
                matched = rule;
            }
        }
        return matched;
    }

    /** 声明仅接受普通标识符，禁止将表达式、限定列或动态SQL注入规则。 */
    private void validateTableRule(DataPermissionTable rule) {
        if (!rule.table().matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?")) {
            throw new IllegalArgumentException("数据权限表名必须为普通表名或schema.table");
        }
        for (String column : List.of(rule.alias(), rule.publicColumn(), rule.orgColumn(), rule.userColumn())) {
            if (!column.isEmpty() && !column.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("数据权限别名及字段必须为普通标识符");
            }
        }
    }

    /** 表级字段优先于外层声明，未声明时使用框架默认字段。 */
    private String resolveColumn(String column, String annotationColumn, String defaultColumn) {
        return StringUtils.hasText(column) ? column
                : (StringUtils.hasText(annotationColumn) ? annotationColumn : defaultColumn);
    }

    private Expression buildOrganizationExpression(Column orgColumn, Set<Long> scopeOrgIds) {
        if (scopeOrgIds.size() == 1) {
            var organizationId = new LongValue(scopeOrgIds.iterator().next());
            return new EqualsTo(orgColumn, organizationId);
        }
        List<Expression> expressions = scopeOrgIds.stream().map(LongValue::new).collect(Collectors.toList());
        var organizationIds = new ParenthesedExpressionList<>(expressions);
        return new InExpression(orgColumn, organizationIds);
    }

    /**
     * 判断是否应该执行数据权限过滤
     */
    private boolean shouldApplyDataPermission() {
        if (properties == null || !Boolean.TRUE.equals(properties.getEnabled())) {
            return false;
        }
        return !RequestContext.getIgnoreDataPermission();
    }

    /**
     * 校验表是否属于忽略数据权限的表
     *
     * @param tableName 表名
     * @return 是否忽略
     */
    public boolean isIgnoreTable(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            return false;
        }
        return ignoreTables.contains(tableName.toUpperCase().trim());
    }

    /**
     * 构造带别名的 Column 对象
     */
    private Column buildColumn(Table table, String columnName) {
        String qualifier = table.getAlias() == null ? table.getName() : table.getAlias().getName();
        var qualifiedTable = new Table(qualifier);
        return new Column(qualifiedTable, columnName);
    }

    private boolean matchesTargetTable(Table table, String configuredAlias) {
        if (!StringUtils.hasText(configuredAlias)) {
            return true;
        }
        String targetAlias = configuredAlias.trim().replaceFirst("\\.$", "");
        String actualAlias = table.getAlias() == null ? table.getName() : table.getAlias().getName();
        return targetAlias.equalsIgnoreCase(actualAlias);
    }

    private Expression appendOr(Expression left, Expression right) {
        return left == null ? right : new OrExpression(left, right);
    }

    private Expression parenthesizeOr(Expression expression) {
        return expression instanceof OrExpression ? new ParenthesedExpressionList<>(expression) : expression;
    }

    /**
     * 当权限范围内无任何可用 ID 时，生成 1 = 0 恒假表达式阻断数据访问
     */
    private Expression buildAlwaysFalseExpression() {
        var one = new LongValue(1);
        var zero = new LongValue(0);
        return new EqualsTo(one, zero);
    }
}
