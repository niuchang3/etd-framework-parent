package org.etd.ai.model.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashSet;

/** 全局模型管理能力；平台管理员维护，普通租户仅查看已启用模型。 */
@Service
@RequiredArgsConstructor
public class ModelService {
    private final ModelMapper mapper;

    /**
     * 分页查询模型；平台管理员可查看停用记录，租户仅查看已启用记录。
     * @param current 从1开始的页码
     * @param size 每页数量
     * @param providerCode 可选服务商字典值
     * @param modelType 可选模型类型字典值
     * @param dataStatus 管理员可选状态筛选；租户始终只返回已启用记录
     * @return 全租户共用的模型管理
     */
    public IPage<ModelEntity> fetchPage(
            long current, long size, String providerCode, String modelType, Integer dataStatus) {
        var page = new Page<ModelEntity>(current, size);
        var query = Wrappers.<ModelEntity>lambdaQuery()
                .eq(dataStatus != null, ModelEntity::getDataStatus, dataStatus)
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

    /**
     * 查询管理详情；普通租户仍不能查看停用模型。
     * @param id 模型标识
     * @return 当前身份可查看的模型
     */
    public ModelEntity fetchModel(Long id) {
        if (!isPlatformAdmin()) {
            return requireModel(id);
        }
        return requireExistingModel(id, false);
    }

    /**
     * 在编排事务中锁定模型，串行化模型删除、关键属性修改与新增连接。
     * @param id 模型标识
     * @return 已锁定模型，包含停用记录
     */
    public ModelEntity lockModel(Long id) {
        return requireExistingModel(id, true);
    }

    /**
     * 新增全局模型，基础状态及审计默认值由框架填充。
     * @param entity 模型定义，不接受租户归属
     * @return 模型标识
     * @throws ApiRuntimeException 向量模型支持维度集合不合法
     * @throws AccessDeniedException 当前用户不是可写的平台管理员
     */
    public Long createModel(ModelEntity entity) {
        requirePlatformAdmin();
        validateSupportedDimensions(entity);
        entity.setVersion(null);
        mapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改已在编排事务中锁定的模型，按版本拒绝并发覆盖。
     * @param entity 带标识及版本的模型定义
     * @throws ApiRuntimeException 版本冲突或支持维度集合不合法
     */
    public void modifyModel(ModelEntity entity) {
        requirePlatformAdmin();
        validateSupportedDimensions(entity);
        var update = Wrappers.<ModelEntity>lambdaUpdate().eq(ModelEntity::getId, entity.getId())
                .set(entity.getLogoPath() == null, ModelEntity::getLogoPath, null)
                .set(entity.getDescription() == null, ModelEntity::getDescription, null)
                .set(entity.getApiKeyUrl() == null, ModelEntity::getApiKeyUrl, null)
                .set(entity.getSetupGuideUrl() == null, ModelEntity::getSetupGuideUrl, null)
                .set(entity.getBaseUrlHelp() == null, ModelEntity::getBaseUrlHelp, null)
                .set(entity.getEmbedding() == null, ModelEntity::getEmbedding, null);
        if (mapper.update(entity, update) != 1) {
            throw new ApiRuntimeException("模型已变化，请刷新重试");
        }
    }

    /**
     * 逻辑删除已锁定且没有有效连接引用的模型。
     * @param id 模型标识
     * @throws ApiRuntimeException 模型已被其他操作删除
     */
    public void removeModel(Long id) {
        requirePlatformAdmin();
        if (mapper.deleteById(id) != 1) {
            throw new ApiRuntimeException("模型不存在或已删除");
        }
    }

    /**
     * 校验可信身份及写入状态，不能仅凭菜单write权限修改全局目录。
     * @throws AccessDeniedException 用户不是平台管理员或身份处于只读状态
     */
    public void requirePlatformAdmin() {
        var user = RequestContext.getUser();
        if (user == null || !user.isPlatformAdmin() || user.isReadOnly()) {
            throw new AccessDeniedException("仅平台管理员可以维护模型管理");
        }
    }

    /** 根据可信身份决定是否允许读取停用目录，不使用请求参数声明管理员。 */
    private boolean isPlatformAdmin() {
        var user = RequestContext.getUser();
        return user != null && user.isPlatformAdmin();
    }

    /** 在调用方事务中按需加行锁，不绕过逻辑删除。 */
    private ModelEntity requireExistingModel(Long id, boolean lock) {
        var query = Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getId, id);
        if (lock) {
            query.last("for update");
        }
        var model = mapper.selectOne(query);
        if (model == null) {
            throw new ApiRuntimeException("模型不存在");
        }
        return model;
    }

    /** 登记模型的能力集合，不把知识库实际选用维度或系统存储限制写成模型的固定输出。 */
    private void validateSupportedDimensions(ModelEntity entity) {
        var dimensions = entity.getEmbedding();
        if (!ModelType.EMBEDDING.equals(entity.getModelType())) {
            if (dimensions != null) {
                throw new ApiRuntimeException("非向量模型不能设置支持维度集合");
            }
            return;
        }
        if (dimensions == null || dimensions.length == 0) {
            throw new ApiRuntimeException("向量模型必须填写支持维度集合");
        }
        var unique = new HashSet<Integer>();
        for (var dimension : dimensions) {
            if (dimension == null || dimension <= 0 || !unique.add(dimension)) {
                throw new ApiRuntimeException("支持维度必须是不重复的正整数");
            }
        }
    }
}
