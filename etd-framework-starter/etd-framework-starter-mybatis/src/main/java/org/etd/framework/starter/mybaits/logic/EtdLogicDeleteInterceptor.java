package org.etd.framework.starter.mybaits.logic;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;

/** 为自定义SELECT补充实体声明的逻辑删除条件，与租户、数据权限开关相互独立。 */
public class EtdLogicDeleteInterceptor extends DataPermissionInterceptor {
    /** MyBatis-Plus自定义忽略项名称；仅显式维护已删除数据的查询可跳过。 */
    public static final String IGNORE_KEY = "logicDelete";

    /** 使用实体元数据生成条件，不按字段名称猜测未知表或CTE是否支持逻辑删除。 */
    public EtdLogicDeleteInterceptor() {
        super((MultiDataPermissionHandler) EtdLogicDeleteInterceptor::createLiveRowExpression);
    }

    /** SELECT在分页前过滤；跳过组织数据权限不应同时跳过逻辑删除。 */
    @Override
    public void beforeQuery(Executor executor, MappedStatement statement, Object parameter,
            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        if (InterceptorIgnoreHelper.willIgnoreOthersByKey(statement.getId(), IGNORE_KEY)) {
            return;
        }
        var sql = PluginUtils.mpBoundSql(boundSql);
        sql.sql(parserSingle(sql.sql(), statement.getId()));
    }

    /** 仅扩展查询，不改写自定义UPDATE或将物理DELETE转换成逻辑删除。 */
    @Override
    public void beforePrepare(StatementHandler statementHandler, Connection connection, Integer timeout) {
        // 内置写操作继续由MyBatis-Plus生成，显式维护SQL保持自身语义。
    }

    /** 根据已注册Mapper的@TableLogic元数据生成带实际SQL别名的未删除条件。 */
    private static Expression createLiveRowExpression(Table table, Expression where, String statementId) {
        TableInfo info = TableInfoHelper.getTableInfo(table.getFullyQualifiedName().replace("\"", ""));
        if (info == null || !info.isWithLogicDelete()) {
            return null;
        }
        String plainSql = info.getLogicDeleteSql(false, true);
        String qualifier = table.getAlias() == null ? table.getFullyQualifiedName() : table.getAlias().getName();
        Expression condition = parseCondition(qualifier + "." + plainSql);
        boolean allowUnqualified = table.getAlias() == null;
        if (containsRequiredConjunct(where, condition.toString(), parseCondition(plainSql).toString(), allowUnqualified)) {
            return null;
        }
        return condition;
    }

    /** 只识别AND链中的必需条件；OR分支出现del_flag不能作为已经安全过滤的依据。 */
    private static boolean containsRequiredConjunct(Expression expression, String qualified,
            String unqualified, boolean allowUnqualified) {
        if (expression == null) {
            return false;
        }
        if (expression instanceof AndExpression and) {
            return containsRequiredConjunct(and.getLeftExpression(), qualified, unqualified, allowUnqualified)
                    || containsRequiredConjunct(and.getRightExpression(), qualified, unqualified, allowUnqualified);
        }
        if (expression instanceof ParenthesedExpressionList<?> parentheses && parentheses.size() == 1) {
            return containsRequiredConjunct(parentheses.get(0), qualified, unqualified, allowUnqualified);
        }
        String sql = expression.toString();
        return qualified.equals(sql) || (allowUnqualified && unqualified.equals(sql));
    }

    /** 元数据声明错误直接失败，禁止解析失败后悄悄返回已删除资料。 */
    private static Expression parseCondition(String sql) {
        try {
            return CCJSqlParserUtil.parseCondExpression(sql);
        } catch (JSQLParserException exception) {
            throw new IllegalArgumentException("实体逻辑删除条件无法解析", exception);
        }
    }
}
