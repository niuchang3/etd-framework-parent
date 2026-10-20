package org.etd.ai.model.biz;

import lombok.RequiredArgsConstructor;

import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** 平台模型目录维护编排，组合模型维护与连接引用校验，不承接租户广场用例。 */
@Service
@RequiredArgsConstructor
public class ModelBizService {
    private final ModelService models;
    private final ModelConnectionService connections;

    /**
     * 修改目录，已建立连接后不允许改变调用标识、服务商或类型。
     * @param entity 管理员提交的模型定义及版本
     */
    @Transactional
    public void modifyModel(ModelEntity entity) {
        models.requirePlatformAdmin();
        var old = models.lockModel(entity.getId());
        if (connections.countConnectionByModel(entity.getId()) > 0 && hasChangedBinding(entity, old)) {
            throw new ApiRuntimeException("模型已被连接器使用，不能修改服务商、调用代码或类型");
        }
        models.modifyModel(entity);
    }

    /**
     * 删除未被任何租户有效连接引用的目录项。
     * @param id 平台模型标识
     */
    @Transactional
    public void removeModel(Long id) {
        models.requirePlatformAdmin();
        models.lockModel(id);
        if (connections.countConnectionByModel(id) > 0) {
            throw new ApiRuntimeException("模型已被连接器引用，不能删除");
        }
        models.removeModel(id);
    }

    /** 已有连接引用的技术身份保持稳定，展示信息和启停状态可继续维护。 */
    private boolean hasChangedBinding(ModelEntity entity, ModelEntity old) {
        return !Objects.equals(entity.getProviderCode(), old.getProviderCode())
                || !Objects.equals(entity.getModelCode(), old.getModelCode())
                || !Objects.equals(entity.getModelType(), old.getModelType());
    }
}
