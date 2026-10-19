package org.etd.ai.model.biz;

import lombok.RequiredArgsConstructor;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 租户选择平台模型后的接入编排；凭证加密由连接能力服务负责。 */
@Service
@RequiredArgsConstructor
public class ModelBizService {
    private final ModelService models;
    private final ModelConnectionService connections;

    /**
     * 校验模型可用后创建租户连接，不发起外部模型调用。
     * @param entity 租户提交的模型及连接配置
     * @param apiKey 只写凭证，不记录到日志
     * @return 连接器标识
     */
    @Transactional
    public Long createConnection(ModelConnectionEntity entity, String apiKey) {
        models.requireModel(entity.getModelId());
        return connections.createConnection(entity, apiKey);
    }

    /**
     * 校验模型可用后修改接入信息，禁止更换连接器绑定的模型。
     * @param entity 连接配置，不包含可信租户字段
     * @param apiKey 新凭证，留空保留原凭证
     * @param version 页面读取的乐观锁版本
     */
    @Transactional
    public void modifyConnection(ModelConnectionEntity entity, String apiKey, Long version) {
        models.requireModel(entity.getModelId());
        connections.modifyConnection(entity, apiKey, version);
    }
}
