package org.etd.ai.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.etd.ai.model.mapper.result.EmbeddingConnectionOption;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.model.entity.ModelConnectionEntity;

/** 租户模型连接数据访问，模型展示字段通过SQL关联查询。 */
public interface ModelConnectionMapper extends BaseMapper<ModelConnectionEntity> {
    /**
     * 关联目录分页查询连接，普通租户由框架拦截器隔离。
     * @param page 分页参数
     * @param modelId 可选模型标识
     * @param dataStatus 可选启停状态
     * @param providerCode 可选服务商字典值，用于连接管理的服务商筛选
     * @return 连接及模型展示信息
     */
    IPage<ModelConnectionEntity> selectConnectionPage(IPage<ModelConnectionEntity> page,
            @Param("modelId") Long modelId, @Param("dataStatus") Integer dataStatus,
            @Param("providerCode") String providerCode);

    /**
     * 查询连接及关联模型信息，不向Controller直接返回VO。
     * @param id 连接标识
     * @return 连接详情，不存在时为空
     */
    ModelConnectionEntity selectConnectionDetail(@Param("id") Long id);

    /** @return 当前租户范围内已启用向量连接器及平台登记的维度 */
    List<EmbeddingConnectionOption> selectEmbeddingConnectionOptionList();
}
