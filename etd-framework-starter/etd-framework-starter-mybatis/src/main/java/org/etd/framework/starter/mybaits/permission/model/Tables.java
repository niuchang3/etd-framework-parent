package org.etd.framework.starter.mybaits.permission.model;

import lombok.Data;
import org.etd.framework.starter.mybaits.permission.constant.DataPermissionsType;

@Data
public class Tables {
    /**
     * 需要权限验证的表名
     */
    private String tableName;
    /**
     * 控制权限的库表 列名
     */
    private String tableColumnName;
    /**
     * 数据权限类型
     */
    private DataPermissionsType dataPermissionsType;
}
