package org.etd.ai.model.mapper.result;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.ai.model.entity.ModelEntity;

/** 模型目录与连接统计的只读查询投影，不用于持久化写入。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelSquareModel extends ModelEntity {
    /** 当前访问范围内未删除连接数量，包含停用连接。 */
    private long connectionCount;
    /** 当前访问范围内已启用连接数量，不表示调用验证成功。 */
    private long enabledConnectionCount;
}
