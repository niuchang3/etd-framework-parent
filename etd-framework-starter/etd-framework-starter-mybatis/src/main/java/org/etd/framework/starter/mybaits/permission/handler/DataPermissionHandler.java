package org.etd.framework.starter.mybaits.permission.handler;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;

/**
 * @author Administrator
 */
public interface DataPermissionHandler {

    /**
     * 获取数据权限SQL片段
     *
     * @param table             所执行的数据库表信息，可以通过此参数获取表名和表别名
     * @param where             where 条件信息
     * @param mappedStatementId Mybatis MappedStatement Id 根据该参数可以判断具体执行方法
     * @return
     */
    Expression getSqlSegment(Table table, Expression where, String mappedStatementId);
}
