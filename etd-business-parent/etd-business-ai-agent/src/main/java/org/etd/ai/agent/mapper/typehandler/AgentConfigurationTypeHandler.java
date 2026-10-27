package org.etd.ai.agent.mapper.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.etd.ai.agent.model.AgentConfiguration;

/** Agent类型化配置的JSONB映射，不启用多态反序列化或复用HTTP时间格式。 */
public class AgentConfigurationTypeHandler extends BaseTypeHandler<AgentConfiguration> {
    /** 独立映射器仅处理明确的配置类型，不接收动态Java类型信息。 */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** @param statement JDBC语句 @param index 参数位置 @param value 配置 @param jdbcType JDBC类型 @throws SQLException 序列化失败 */
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index,
            AgentConfiguration value, JdbcType jdbcType) throws SQLException {
        try {
            String json = JSON.writeValueAsString(value);
            statement.setObject(index, json, Types.OTHER);
        } catch (JsonProcessingException exception) {
            throw new SQLException("Agent配置序列化失败", exception);
        }
    }

    /** @param result 结果集 @param name 列名 @return 配置 @throws SQLException 读取失败 */
    @Override
    public AgentConfiguration getNullableResult(ResultSet result, String name) throws SQLException {
        String json = result.getString(name);
        return readConfiguration(json);
    }

    /** @param result 结果集 @param index 列位置 @return 配置 @throws SQLException 读取失败 */
    @Override
    public AgentConfiguration getNullableResult(ResultSet result, int index) throws SQLException {
        String json = result.getString(index);
        return readConfiguration(json);
    }

    /** @param statement 存储过程 @param index 输出位置 @return 配置 @throws SQLException 读取失败 */
    @Override
    public AgentConfiguration getNullableResult(CallableStatement statement, int index) throws SQLException {
        String json = statement.getString(index);
        return readConfiguration(json);
    }

    /** 只还原配置对象，不把数据库JSON null转换成可运行配置。 */
    private AgentConfiguration readConfiguration(String json) throws SQLException {
        if (json == null) {
            return null;
        }
        try {
            return JSON.readValue(json, AgentConfiguration.class);
        } catch (JsonProcessingException exception) {
            throw new SQLException("Agent配置反序列化失败", exception);
        }
    }
}
