package org.etd.framework.starter.mybaits.permission.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.google.common.collect.Sets;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.context.DataPermissionContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
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

    private final EtdMyBatisPlusProperties.DataPermissionProperties properties;
    private final Set<String> ignoreTables = Sets.newHashSet();

    public EtdDataPermissionHandler(EtdMyBatisPlusProperties.DataPermissionProperties properties) {
        this.properties = properties;
        if (properties != null && !CollectionUtils.isEmpty(properties.getIgnoreTables())) {
            addIgnoreTables(properties.getIgnoreTables());
        }
    }

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
    /**
     * 获取 SqlSegment 属性值
     *
     * @param table 参数 table
     * @param where 参数 where
     * @param mappedStatementId 参数 mappedStatementId
     * @return 处理结果
     */
    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        if (!shouldApplyDataPermission() || isIgnoreTable(table.getName())) {
            return null;
        }

        UserDetails user = RequestContext.getUser();
        if (user != null && user.isAdmin()) {
            return null;
        }

        DataPermission annotation = DataPermissionContextHolder.peek();
        if (annotation != null && (!annotation.enable() || !matchesTargetTable(table, annotation.alias()))) {
            return null;
        }
        if (user == null) {
            return buildAlwaysFalseExpression();
        }

        Set<String> permissionTypes = resolvePermissionTypes(user);
        if (permissionTypes.contains(BasicConstant.PermissionType.ALL.getCode())) {
            return null;
        }
        String orgColumnName = (annotation != null && StringUtils.hasText(annotation.orgColumn()))
                ? annotation.orgColumn()
                : properties.getDefaultOrgColumn();
        String userColumnName = (annotation != null && StringUtils.hasText(annotation.userColumn()))
                ? annotation.userColumn()
                : properties.getDefaultUserColumn();

        Expression permissionExpression = null;
        if (permissionTypes.contains(BasicConstant.PermissionType.SELF.getCode()) && user.getId() != null) {
            permissionExpression = new EqualsTo(buildColumn(table, userColumnName), new LongValue(user.getId()));
        }
        if (hasOrganizationPermission(permissionTypes) && !CollectionUtils.isEmpty(user.getScopeOrgIds())) {
            Expression organizationExpression = buildOrganizationExpression(
                    buildColumn(table, orgColumnName), user.getScopeOrgIds());
            permissionExpression = appendOr(permissionExpression, organizationExpression);
        }
        return permissionExpression == null ? buildAlwaysFalseExpression() : parenthesizeOr(permissionExpression);
    }

    private Set<String> resolvePermissionTypes(UserDetails user) {
        Set<String> permissionTypes = user.getPermissionTypes();
        if (!CollectionUtils.isEmpty(permissionTypes)) {
            return permissionTypes;
        }
        Set<String> legacyPermissionTypes = new LinkedHashSet<>();
        if (StringUtils.hasText(user.getPermissionType())) {
            legacyPermissionTypes.add(user.getPermissionType());
        }
        return legacyPermissionTypes;
    }

    private boolean hasOrganizationPermission(Set<String> permissionTypes) {
        return permissionTypes.contains(BasicConstant.PermissionType.ORGANIZATION.getCode())
                || permissionTypes.contains(BasicConstant.PermissionType.ORGANIZATION_AND_SUBORDINATE.getCode())
                || permissionTypes.contains(BasicConstant.PermissionType.CUSTOM_ORGANIZATION.getCode());
    }

    private Expression buildOrganizationExpression(Column orgColumn, Set<Long> scopeOrgIds) {
        if (scopeOrgIds.size() == 1) {
            return new EqualsTo(orgColumn, new LongValue(scopeOrgIds.iterator().next()));
        }
        List<Expression> expressions = scopeOrgIds.stream().map(LongValue::new).collect(Collectors.toList());
        return new InExpression(orgColumn, new ParenthesedExpressionList<>(expressions));
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
        return new Column(new Table(qualifier), columnName);
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
        return new EqualsTo(new LongValue(1), new LongValue(0));
    }
}
