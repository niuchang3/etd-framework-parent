package org.etd.ai.model.mapper.typehandler;

import org.apache.ibatis.type.ArrayTypeHandler;

import java.sql.Array;
import java.sql.SQLException;

/** 使用JDBC原生整数数组，统一数据库驱动返回的数组元素类型，不将维度集合存为字符串。 */
public class EmbeddingDimensionsTypeHandler extends ArrayTypeHandler {
    /**
     * 读取维度集合并释放JDBC数组资源，保留SQL空值语义。
     * @param array JDBC数组，允许为空
     * @return 支持维度的整数数组；数据库为空时返回null
     * @throws SQLException 数据库数组读取失败
     */
    @Override
    protected Integer[] extractArray(Array array) throws SQLException {
        Object[] values = (Object[]) super.extractArray(array);
        if (values == null) {
            return null;
        }
        Integer[] dimensions = new Integer[values.length];
        for (int i = 0; i < values.length; i++) {
            dimensions[i] = values[i] == null ? null : ((Number) values[i]).intValue();
        }
        return dimensions;
    }
}
