package org.etd.framework.starter.mybaits.permission.handler;

import com.google.common.collect.Maps;
import lombok.Data;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import org.etd.framework.starter.mybaits.permission.DataPermissionApi;
import org.etd.framework.starter.mybaits.permission.constant.DataPermissionsType;
import org.etd.framework.starter.mybaits.permission.model.Tables;

import java.util.Map;

@Data
public class DefaultDataPermissionHandler implements DataPermissionHandler {
    /**
     * 部门权限表
     */
    private Map<String, String> deptTables = Maps.newConcurrentMap();
    /**
     * 用户权限表
     */
    private Map<String, String> userTables = Maps.newConcurrentMap();


    private DataPermissionApi dataPermissionApi;

    public DefaultDataPermissionHandler(DataPermissionApi dataPermissionApi) {
        this.dataPermissionApi = dataPermissionApi;
    }

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {


        return null;
    }


    public void addDataPermissionTable(Tables tables) {
        if (DataPermissionsType.USER.equals(tables.getDataPermissionsType())) {
            addUserPermissionTable(tables.getTableName(), tables.getTableColumnName());
        }
        if (DataPermissionsType.DEPARTMENT.equals(tables.getDataPermissionsType())) {
            addDeptPermissionTable(tables.getTableName(), tables.getTableColumnName());
        }
    }

    public void addUserPermissionTable(String tableName, String tableColumnName) {
        userTables.put(tableName, tableColumnName);
    }

    public void addDeptPermissionTable(String tableName, String tableColumnName) {
        deptTables.put(tableName, tableColumnName);
    }
}
