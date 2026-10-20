package org.etd.ai.knowledge.mapper.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.etd.ai.knowledge.model.UploadPart;
import java.sql.*;
import java.util.List;

/** PostgreSQL jsonb上传回执转换，使用OTHER而不是varchar绑定，保留Record元素类型。 */
public class UploadPartsTypeHandler extends BaseTypeHandler<List<UploadPart>> {
    /** 不含时间属性，使用独立只读Jackson映射器。 */
    private static final ObjectMapper JSON = new ObjectMapper();
    /** 明确集合元素类型，避免反序列化成Map。 */
    private static final TypeReference<List<UploadPart>> TYPE = new TypeReference<>() {};
    /** @param statement SQL参数 @param index 位置 @param value 上传回执 @param jdbcType JDBC类型 */
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<UploadPart> value, JdbcType jdbcType) throws SQLException {
        try {
            statement.setObject(index, JSON.writeValueAsString(value), Types.OTHER);
        } catch (Exception exception) {
            throw new SQLException("上传分片回执序列化失败", exception);
        }
    }
    /** @param result 查询结果 @param name 字段 @return 分片回执 */
    @Override
    public List<UploadPart> getNullableResult(ResultSet result, String name) throws SQLException {
        return parse(result.getString(name));
    }
    /** @param result 查询结果 @param index 字段位置 @return 分片回执 */
    @Override
    public List<UploadPart> getNullableResult(ResultSet result, int index) throws SQLException {
        return parse(result.getString(index));
    }
    /** @param statement 存储过程 @param index 字段位置 @return 分片回执 */
    @Override
    public List<UploadPart> getNullableResult(CallableStatement statement, int index) throws SQLException {
        return parse(statement.getString(index));
    }
    /** 空数据库值按空清单读取，非法JSON明确报错。 */
    private List<UploadPart> parse(String value) throws SQLException {
        try {
            return value == null ? List.of() : JSON.readValue(value, TYPE);
        } catch (Exception exception) {
            throw new SQLException("上传分片回执反序列化失败", exception);
        }
    }
}
