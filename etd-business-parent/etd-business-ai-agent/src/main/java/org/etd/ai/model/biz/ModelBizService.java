package org.etd.ai.model.biz;

import lombok.RequiredArgsConstructor;

import org.etd.ai.model.entity.ModelConfigEntity;
import org.etd.ai.model.service.ModelConfigService;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 模型和连接的跨能力写入编排，锁定连接防止并发删除后新建模型。 */
@Service
@RequiredArgsConstructor
public class ModelBizService {
    private final ModelConfigService models;
    private final ModelConnectionService connections;

    /**
     * 创建前校验并锁定同租户连接。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @return 业务记录的雪花标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public Long createModel(ModelConfigEntity e) {
        connections.requireConnection(e.getConnectionId(), true);
        return models.createModel(e);
    }

    /**
     * 修改时锁定目标连接并校验模型版本。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @param version 页面读取的当前版本，用于拒绝并发覆盖
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public void modifyModel(ModelConfigEntity e, Long version) {
        connections.requireConnection(e.getConnectionId(), true);
        models.modifyModel(e, version);
    }

    /**
     * 删除连接前检查仍在使用的模型。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public void removeConnection(Long id) {
        connections.requireConnection(id, true);
        if (models.countModelByConnection(id) > 0) {
            throw new ApiRuntimeException("连接仍有模型配置，不能删除");
        }
        connections.removeConnection(id);
    }
}
