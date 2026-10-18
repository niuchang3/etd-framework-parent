package org.etd.ai.model.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.etd.ai.knowledge.constant.AiStage;
import org.etd.ai.model.entity.ModelConfigEntity;
import org.etd.ai.model.mapper.ModelConfigMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** 模型草稿配置能力，维度仅后端实测写入，页面不可编辑。 */
@Service
@RequiredArgsConstructor
public class ModelConfigService {
    private final ModelConfigMapper mapper;

    /**
     * 按类型分页取得租户模型配置。
     *
     * @param current 从1开始的页码
     * @param size 每页记录数或预期字节数，以方法用途为准
     * @param type 可选模型主能力类型代码
     * @param dataStatus 可选启停状态；不传时不过滤状态
     * @return 符合当前租户及查询条件的分页结果
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public IPage<ModelConfigEntity> fetchPage(
            long current, long size, String type, Integer dataStatus) {
        var page = new Page<ModelConfigEntity>(current, size);
        var query =
                Wrappers.<ModelConfigEntity>lambdaQuery()
                        .eq(type != null, ModelConfigEntity::getModelType, type)
                        .eq(dataStatus != null, ModelConfigEntity::getDataStatus, dataStatus)
                        .orderByDesc(ModelConfigEntity::getCreateTime, ModelConfigEntity::getId);
        return mapper.selectPage(page, query);
    }

    /**
     * 取得配置详情，禁止跨租户引用。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 通过存在性及访问范围校验的业务记录
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public ModelConfigEntity requireModel(Long id) {
        var query = Wrappers.<ModelConfigEntity>lambdaQuery().eq(ModelConfigEntity::getId, id);
        var e = mapper.selectOne(query);
        if (e == null) {
            throw new ApiRuntimeException("模型配置不存在");
        }
        return e;
    }

    /**
     * 创建未验证的模型配置。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @return 业务记录的雪花标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public Long createModel(ModelConfigEntity e) {
        validateDimensions(e);
        mapper.insert(e);
        return e.getId();
    }

    /**
     * 按版本修改草稿。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @param version 页面读取的当前版本，用于拒绝并发覆盖
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void modifyModel(ModelConfigEntity e, Long version) {
        var old = requireModel(e.getId());
        // 维度只能由后端实测写入；页面编辑不覆盖已有实测值，也不允许手工声明。
        if (AiStage.EMBEDDING.equals(e.getModelType())
                && AiStage.EMBEDDING.equals(old.getModelType())) {
            e.setEmbeddingDimensions(old.getEmbeddingDimensions());
        }
        validateDimensions(e);
        if (mapper.selectKnowledgeBaseCount(e.getId()) > 0
                && (!Objects.equals(old.getConnectionId(), e.getConnectionId())
                        || !Objects.equals(old.getModelCode(), e.getModelCode())
                        || !Objects.equals(old.getModelType(), e.getModelType())
                        || !Objects.equals(
                                old.getEmbeddingDimensions(), e.getEmbeddingDimensions()))) {
            throw new ApiRuntimeException("模型已被知识库引用，不能修改向量空间相关配置");
        }
        e.setVersion(version);
        if (mapper.updateById(e) != 1) {
            throw new ApiRuntimeException("模型配置已变化，请刷新重试");
        }
    }

    /**
     * 查询连接是否仍有模型，逻辑删除数据不计入页面占用。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 符合当前租户及查询条件的记录数量
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public long countModelByConnection(Long id) {
        var query =
                Wrappers.<ModelConfigEntity>lambdaQuery()
                        .eq(ModelConfigEntity::getConnectionId, id);
        return mapper.selectCount(query);
    }

    /**
     * 删除草稿配置，已有知识库引用时由外键拒绝破坏关联。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void removeModel(Long id) {
        requireModel(id);
        if (mapper.selectKnowledgeBaseCount(id) > 0) {
            throw new ApiRuntimeException("模型仍被知识库引用，不能删除");
        }
        mapper.deleteById(id);
    }

    private void validateDimensions(ModelConfigEntity e) {
        if (!AiStage.EMBEDDING.equals(e.getModelType()) && e.getEmbeddingDimensions() != null) {
            throw new ApiRuntimeException("只有向量模型可以配置声明维度");
        }
    }
}
