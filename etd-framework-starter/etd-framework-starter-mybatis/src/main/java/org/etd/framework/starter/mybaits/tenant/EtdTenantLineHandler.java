package org.etd.framework.starter.mybaits.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import org.etd.framework.common.core.user.UserDetails;
import com.google.common.collect.Sets;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.etd.framework.common.core.context.model.RequestContext;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Set;

public class EtdTenantLineHandler implements TenantLineHandler {

    private final String tenantIdColumn;

    private final Set<String> ignoreTables = Sets.newHashSet();


    public EtdTenantLineHandler(String tenantIdColumn) {
        this.tenantIdColumn = tenantIdColumn;
    }


    public EtdTenantLineHandler addIgnoreTables(String tableName) {
        ignoreTables.add(tableName.toUpperCase());
        return this;
    }

    /**
     * 添加 Ignore Tables
     *
     * @param tableNames 参数 tableNames
     */
    public void addIgnoreTables(List<String> tableNames){
        for (String tableName : tableNames) {
            addIgnoreTables(tableName.toUpperCase());
        }
    }

    /**
     * 获取当前租户
     *
     * @return
     */
    /**
     * 获取 TenantId 属性值
     *
     * @return 处理结果
     */
    @Override
    public Expression getTenantId() {
        if(ObjectUtils.isEmpty(RequestContext.getTenantCode())){
            return new NullValue();
        }
        return new LongValue(RequestContext.getTenantCode());
    }

    /**
     * 数据库租户列名
     *
     * @return
     */
    /**
     * 获取 TenantIdColumn 属性值
     *
     * @return 处理结果
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
    /**
     * ignore Table
     *
     * @param tableName 参数 tableName
     * @return 处理结果
     */
    @Override
    public boolean ignoreTable(String tableName) {
        boolean ignoreTenant = RequestContext.getIgnoreTenant();
        if(ignoreTenant){
            return true;
        }
        if (ignoreTables.contains(tableName.toUpperCase())) {
            return true;
        }
        return false;
    }
}
