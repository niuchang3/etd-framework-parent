package org.etd.ai.knowledge.service;

import lombok.RequiredArgsConstructor;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Objects;

/** 仅校验知识库保存的模型配置，不发起模型请求或向量处理。 */
@Service
@RequiredArgsConstructor
public class KnowledgeModelSettingsService {
    private final ModelConnectionMapper connections;
    private final ModelMapper models;

    /** @param connectionId 模型连接 @param type 所需能力类型 @param dimensions 可选向量维度 */
    public void validate(Long connectionId, String type, Integer dimensions) {
        ModelConnectionEntity connection = requireConnection(connectionId);
        ModelEntity model = requireModel(connection, type);
        if (dimensions != null && (model.getEmbedding() == null || !Arrays.asList(model.getEmbedding()).contains(dimensions))) {
            throw new ApiRuntimeException("所选维度不在模型支持范围内");
        }
    }

    private ModelConnectionEntity requireConnection(Long id) {
        ModelConnectionEntity connection = id == null ? null : connections.selectById(id);
        if (connection == null || !Objects.equals(connection.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("模型连接不存在或已停用");
        }
        return connection;
    }

    private ModelEntity requireModel(ModelConnectionEntity connection, String type) {
        ModelEntity model = models.selectById(connection.getModelId());
        if (model == null || !type.equals(model.getModelType())
                || !Objects.equals(model.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("模型不存在、已停用或类型不匹配");
        }
        return model;
    }

}
