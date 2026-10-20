package org.etd.ai.model.biz;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.result.ModelSquareModel;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelService;
import org.etd.ai.model.service.ModelSquareService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 首页模型广场用例编排，统一组合模型查询、接入配置和连接管理，不直接访问Mapper。 */
@Service
@RequiredArgsConstructor
public class ModelSquareBizService {
    private final ModelSquareService squareQueries;
    private final ModelService models;
    private final ModelConnectionService connections;

    /**
     * 分页浏览平台已启用模型及当前访问范围内的接入统计。
     * @param current 从1开始的页码
     * @param size 每页数量
     * @param providerCode 可选服务商字典值
     * @param modelType 可选模型主能力类型
     * @param keyword 可选模型名称或代码搜索文本
     * @param connected true仅已接入、false仅未接入、null全部
     * @return 首页模型分页，不包含连接地址或凭证
     */
    public IPage<ModelSquareModel> fetchModelPage(long current, long size, String providerCode,
            String modelType, String keyword, Boolean connected) {
        return squareQueries.fetchModelPage(current, size, providerCode, modelType, keyword, connected);
    }

    /**
     * 获取可供租户接入的模型详情及接入指引。
     * @param id 平台模型标识
     * @return 已启用模型，不要求额外的模型管理菜单权限
     * @throws ApiRuntimeException 模型不存在、已删除或已停用
     */
    public ModelEntity fetchModel(Long id) {
        return models.requireModel(id);
    }

    /**
     * 分页查看广场内已有连接，包括平台停用模型的连接。
     * @param current 从1开始的页码
     * @param size 每页数量
     * @param modelId 可选模型标识
     * @param dataStatus 可选连接启停状态
     * @param providerCode 可选服务商字典值
     * @return 当前访问范围内的连接分页，由Controller转换为脱敏响应
     */
    public IPage<ModelConnectionEntity> fetchConnectionPage(long current, long size, Long modelId,
            Integer dataStatus, String providerCode) {
        return connections.fetchPage(current, size, modelId, dataStatus, providerCode);
    }

    /**
     * 查询当前访问范围内的模型连接详情。
     * @param id 连接标识
     * @return 待转换为脱敏响应的连接记录
     * @throws ApiRuntimeException 连接不存在或不在当前访问范围
     */
    public ModelConnectionEntity fetchConnection(Long id) {
        return connections.fetchConnection(id);
    }

    /**
     * 校验模型可用后创建该模型的连接，每条连接仅绑定一个模型。
     * @param entity 模型及接入配置，可信归属由框架填充
     * @param apiKey 只写凭证，由连接能力服务加密
     * @return 新建连接标识；保存不表示完成真实模型调用验证
     * @throws ApiRuntimeException 模型不可接入或连接参数不合法
     */
    @Transactional
    public Long createConnection(ModelConnectionEntity entity, String apiKey) {
        models.lockModel(entity.getModelId());
        models.requireModel(entity.getModelId());
        return connections.createConnection(entity, apiKey);
    }

    /**
     * 校验模型可用后修改接入配置，不允许切换绑定模型。
     * @param entity 带连接标识的接入配置
     * @param apiKey 新凭证，留空保留已有密文
     * @param version 当前连接版本
     * @throws ApiRuntimeException 模型不可接入、连接不存在或版本冲突
     */
    @Transactional
    public void modifyConnection(ModelConnectionEntity entity, String apiKey, Long version) {
        models.lockModel(entity.getModelId());
        models.requireModel(entity.getModelId());
        connections.modifyConnection(entity, apiKey, version);
    }

    /**
     * 串行校验模型状态后启停连接；停用模型的已有连接仍可停用。
     * @param id 连接标识
     * @param dataStatus 目标启停状态
     * @param version 当前连接版本
     * @throws ApiRuntimeException 连接不存在、版本冲突或试图启用停用模型的连接
     */
    @Transactional
    public void modifyConnectionStatus(Long id, Integer dataStatus, Long version) {
        var connection = connections.requireConnection(id, false);
        var modelId = connection.getModelId();
        // 与新增、修改接入保持先锁模型再锁连接的顺序，不改变框架租户及管理员访问边界。
        models.lockModel(modelId);
        if (dataStatus == BasicConstant.DataStatus.ENABLED_CODE) {
            models.requireModel(modelId);
        }
        connections.modifyConnectionStatus(id, dataStatus, version);
    }

    /**
     * 删除当前访问范围内未被知识库引用的连接。
     * @param id 连接标识
     * @throws ApiRuntimeException 连接不存在或仍被知识库引用
     */
    @Transactional
    public void removeConnection(Long id) {
        connections.removeConnection(id);
    }
}
