package org.etd.framework.starter.mybaits.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.etd.framework.common.core.context.model.RequestContext;

public class EtdTenantLineHandler implements TenantLineHandler {

    private final String tenantIdColumn;

    public EtdTenantLineHandler(String tenantIdColumn) {
        this.tenantIdColumn = tenantIdColumn;
    }

    /**
     * 获取当前租户
     *
     * @return
     */
    @Override
    public Expression getTenantId() {
        return new LongValue(RequestContext.getTenantCode());
    }

    /**
     * 数据库租户列名
     *
     * @return
     */
    @Override
    public String getTenantIdColumn() {
        return tenantIdColumn;
    }

    /**
     * 是否忽略租户化
     *
     * @param tableName 表名
     * @return
     */
    @Override
    public boolean ignoreTable(String tableName) {
        return RequestContext.getIgnoreTenant();
    }
}
