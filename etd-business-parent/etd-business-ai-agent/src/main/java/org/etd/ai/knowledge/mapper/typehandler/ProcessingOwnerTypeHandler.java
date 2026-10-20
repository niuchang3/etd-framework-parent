package org.etd.ai.knowledge.mapper.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;
import java.util.UUID;

/** PostgreSQL UUID处理租约类型转换，不依赖数据库驱动具体类。 */
public class ProcessingOwnerTypeHandler extends BaseTypeHandler<UUID> {
    /** @param statement 参数 @param index 位置 @param value 租约UUID @param jdbcType 类型 */
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, UUID value, JdbcType jdbcType) throws SQLException {
        statement.setObject(index, value, Types.OTHER);
    }
    /** @param result 结果 @param name 字段 @return UUID租约 */
    @Override
    public UUID getNullableResult(ResultSet result, String name) throws SQLException {
        return parse(result.getString(name));
    }
    /** @param result 结果 @param index 位置 @return UUID租约 */
    @Override
    public UUID getNullableResult(ResultSet result, int index) throws SQLException {
        return parse(result.getString(index));
    }
    /** @param statement 过程 @param index 位置 @return UUID租约 */
    @Override
    public UUID getNullableResult(CallableStatement statement, int index) throws SQLException {
        return parse(statement.getString(index));
    }
    /** 数据库空值保持为空，非法UUID由驱动读取过程明确失败。 */
    private UUID parse(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
