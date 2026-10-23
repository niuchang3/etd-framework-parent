package org.etd.ai.knowledge.mapper.typehandler;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

/** 分片参数和来源共用的JSONB转换，Duration使用ISO-8601字符串。 */
public abstract class KnowledgeJsonTypeHandler<T> extends BaseTypeHandler<T> {
    /** 独立的JSONB映射器，不继承业务多态序列化配置。 */
    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules()
            .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS).build();
    /** 显式反序列化目标，保留记录和嵌套参数的类型。 */
    private final Class<T> type;
    /** @param type 当前JSON字段的目标类型 */
    protected KnowledgeJsonTypeHandler(Class<T> type) {
        this.type = type;
    }
    /** @param statement JDBC语句 @param index 参数位置 @param value 字段内容 @param jdbcType JDBC类型 */
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, T value, JdbcType jdbcType) throws SQLException {
        try {
            statement.setObject(index, JSON.writeValueAsString(value), Types.OTHER);
        } catch (Exception exception) {
            throw new SQLException("分片JSON序列化失败", exception);
        }
    }
    /** @param result 结果集 @param name 列名 @return 还原的字段值 */
    @Override
    public T getNullableResult(ResultSet result, String name) throws SQLException {
        return read(result.getString(name));
    }
    /** @param result 结果集 @param index 列位置 @return 还原的字段值 */
    @Override
    public T getNullableResult(ResultSet result, int index) throws SQLException {
        return read(result.getString(index));
    }
    /** @param statement 存储过程 @param index 输出位置 @return 还原的字段值 */
    @Override
    public T getNullableResult(CallableStatement statement, int index) throws SQLException {
        return read(statement.getString(index));
    }
    private T read(String value) throws SQLException {
        if (value == null) { return null; }
        try {
            return JSON.readValue(value, type);
        } catch (Exception exception) {
            throw new SQLException("分片JSON反序列化失败", exception);
        }
    }
}
