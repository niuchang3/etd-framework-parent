package org.etd.framework.starter.mybaits.permission.handler;

import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Table;

/** 区分查询与写入的权限入口，复用MyBatis-Plus对JOIN、子查询及集合查询的遍历。 */
public class EtdDataPermissionInterceptor extends DataPermissionInterceptor {
    /** 统一权限条件生成器，不保存请求状态，可由多个线程共享。 */
    private final EtdDataPermissionHandler handler;

    /**
     * @param handler 框架表级数据权限处理器
     */
    public EtdDataPermissionInterceptor(EtdDataPermissionHandler handler) {
        super(handler);
        this.handler = handler;
    }

    /** SELECT遍历每个真实表引用时允许应用显式声明的公开读取规则。 */
    @Override
    public Expression buildTableExpression(Table table, Expression where, String mappedStatementId) {
        return handler.getSqlSegment(table, where, mappedStatementId, true);
    }

    /** UPDATE和DELETE始终使用严格角色范围，防止公开标记授予编辑或删除能力。 */
    @Override
    protected Expression getUpdateOrDeleteExpression(Table table, Expression where, String mappedStatementId) {
        Expression permission = handler.getSqlSegment(table, where, mappedStatementId, false);
        if (permission == null) {
            return where;
        }
        return where == null ? permission : new AndExpression(
                new ParenthesedExpressionList<>(where), permission);
    }
}
