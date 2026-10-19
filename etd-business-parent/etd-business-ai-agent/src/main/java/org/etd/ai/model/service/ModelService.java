package org.etd.ai.model.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

/** 全局模型只读能力；新增、调整目录由开发人员维护初始化数据完成。 */
@Service
@RequiredArgsConstructor
public class ModelService {
    private final ModelMapper mapper;

    /**
     * 分页查询已启用模型，按服务商及主能力类型筛选。
     * @param current 从1开始的页码
     * @param size 每页数量
     * @param providerCode 可选服务商字典值
     * @param modelType 可选模型类型字典值
     * @return 全租户共用的模型目录
     */
    public IPage<ModelEntity> fetchPage(long current, long size, String providerCode, String modelType) {
        var page = new Page<ModelEntity>(current, size);
        var query = Wrappers.<ModelEntity>lambdaQuery()
                .eq(ModelEntity::getDataStatus, BasicConstant.DataStatus.ENABLED_CODE)
                .eq(providerCode != null, ModelEntity::getProviderCode, providerCode)
                .eq(modelType != null, ModelEntity::getModelType, modelType)
                .orderByAsc(ModelEntity::getSort, ModelEntity::getId);
        return mapper.selectPage(page, query);
    }

    /**
     * 取得可供接入的模型；停用或已删除的目录项不能新增接入。
     * @param id 平台模型标识
     * @return 已启用模型
     * @throws ApiRuntimeException 模型不存在或已停用
     */
    public ModelEntity requireModel(Long id) {
        var query = Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getId, id)
                .eq(ModelEntity::getDataStatus, BasicConstant.DataStatus.ENABLED_CODE);
        var model = mapper.selectOne(query);
        if (model == null) {
            throw new ApiRuntimeException("模型不存在或已停用");
        }
        return model;
    }
}
