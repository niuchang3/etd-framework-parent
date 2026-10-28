package org.etd.ai.agent.mapper.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.etd.ai.agent.model.AgentCitation;

/** 引用来源数组的JSONB边界，不保存检索正文或认证上下文。 */
public class AgentCitationTypeHandler extends BaseTypeHandler<List<AgentCitation>> {
    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();
    private static final TypeReference<List<AgentCitation>> TYPE = new TypeReference<>() { };

    /** @param statement JDBC语句 @param index 位置 @param value 引用数组 @param jdbcType JDBC类型 */
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<AgentCitation> value,
            JdbcType jdbcType) throws SQLException {
        try {
            statement.setObject(index, JSON.writeValueAsString(value), Types.OTHER);
        } catch (Exception exception) {
            throw new SQLException("引用来源序列化失败", exception);
        }
    }
    /** @param result 结果集 @param name 列名 @return 引用数组 */
    @Override
    public List<AgentCitation> getNullableResult(ResultSet result, String name) throws SQLException {
        return read(result.getString(name));
    }
    /** @param result 结果集 @param index 列位置 @return 引用数组 */
    @Override
    public List<AgentCitation> getNullableResult(ResultSet result, int index) throws SQLException {
        return read(result.getString(index));
    }
    /** @param statement 存储过程 @param index 位置 @return 引用数组 */
    @Override
    public List<AgentCitation> getNullableResult(CallableStatement statement, int index) throws SQLException {
        return read(statement.getString(index));
    }
    private List<AgentCitation> read(String value) throws SQLException {
        try {
            return value == null ? List.of() : JSON.readValue(value, TYPE);
        } catch (Exception exception) {
            throw new SQLException("引用来源反序列化失败", exception);
        }
    }
}
