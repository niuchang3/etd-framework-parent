package org.etd.ai.model.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.model.constant.ModelProtocol;
import org.etd.ai.model.constant.ModelType;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

/** 租户模型连接能力，不发起探测或模型调用。 */
@Service
@RequiredArgsConstructor
public class ModelConnectionService {
    /** 顺序请求和并发唯一键冲突统一使用的重复绑定提示。 */
    private static final String MODEL_ALREADY_BOUND_MESSAGE = "该模型已绑定连接器，请修改已有连接器";

    private final ModelConnectionMapper mapper;
    private final ModelCredentialCipher cipher;

    /**
     * 查询Agent编辑页面可选聊天连接，在数据库端同时过滤连接及模型启停状态。
     * @param current 页码
     * @param size 页大小
     * @param keyword 可选模型名称关键字
     * @return 不含地址及凭证的连接记录
     */
    public IPage<ModelConnectionEntity> fetchChatConnectionPage(long current, long size, String keyword) {
        var page = new Page<ModelConnectionEntity>(current, size);
        return mapper.selectAvailableChatPage(page, keyword, ModelType.CHAT, BasicConstant.DataStatus.ENABLED_CODE);
    }

    /**
     * 分页查询连接，Controller仅输出脱敏VO。
     *
     * @param current 从1开始的页码
     * @param size 每页记录数或预期字节数，以方法用途为准
     * @param modelId 可选平台模型标识
     * @param dataStatus 可选启停状态；不传时不过滤状态
     * @param providerCode 可选服务商字典值
     * @return 符合当前租户及查询条件的分页结果
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public IPage<ModelConnectionEntity> fetchPage(
            long current, long size, Long modelId, Integer dataStatus, String providerCode) {
        var page = new Page<ModelConnectionEntity>(current, size);
        return mapper.selectConnectionPage(page, modelId, dataStatus, providerCode);
    }

    /**
     * 要求同租户连接存在，跨表编排时可加行锁。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param lock 是否在调用方事务内获取数据库行锁
     * @return 通过存在性及访问范围校验的业务记录
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public ModelConnectionEntity requireConnection(Long id, boolean lock) {
        var q = Wrappers.<ModelConnectionEntity>lambdaQuery().eq(ModelConnectionEntity::getId, id);
        if (lock) {
            q.last("FOR UPDATE");
        }
        var e = mapper.selectOne(q);
        if (e == null) {
            throw new ApiRuntimeException("模型连接不存在");
        }
        return e;
    }

    /**
     * 创建连接并加密凭证；同一租户的一个模型仅允许一个未删除连接，API Key只写不读。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @param apiKey 待加密凭证或配置值，不写入日志或普通响应
     * @return 业务记录的雪花标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public Long createConnection(ModelConnectionEntity e, String apiKey) {
        validateConnection(e);
        requireUnboundModel(e.getModelId());
        e.setId(IdWorker.getId());
        populateCredential(e, apiKey, null);
        try {
            mapper.insert(e);
        } catch (DuplicateKeyException exception) {
            // 唯一索引负责并发写入的最终约束，重复接入返回业务提示而非数据库错误。
            throw new ApiRuntimeException(MODEL_ALREADY_BOUND_MESSAGE);
        }
        return e.getId();
    }

    /** 校验新连接所属租户的模型绑定唯一性；显式租户条件用于定位唯一键，不限制管理员的查询权限。 */
    private void requireUnboundModel(Long modelId) {
        var query = Wrappers.<ModelConnectionEntity>lambdaQuery()
                .eq(ModelConnectionEntity::getTenantId, RequestContext.getTenantCode())
                .eq(ModelConnectionEntity::getModelId, modelId);
        // 停用仍占用绑定；逻辑删除记录由MyBatis-Plus自动排除。
        if (mapper.exists(query)) {
            throw new ApiRuntimeException(MODEL_ALREADY_BOUND_MESSAGE);
        }
    }

    /**
     * 修改连接，空API Key保留现有密文，显式none清空。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @param apiKey 待加密凭证或配置值，不写入日志或普通响应
     * @param version 页面读取的当前版本，用于拒绝并发覆盖
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public void modifyConnection(ModelConnectionEntity e, String apiKey, Long version) {
        var old = requireConnection(e.getId(), true);
        if (!old.getModelId().equals(e.getModelId())) {
            throw new ApiRuntimeException("连接器不能切换模型，请重新创建连接");
        }
        validateConnection(e);
        populateCredential(e, apiKey, old);
        e.setVersion(version);
        if (mapper.updateById(e) != 1) {
            throw new ApiRuntimeException("连接已变化，请刷新重试");
        }
    }

    /**
     * 按版本修改连接状态，加载完整记录以保持原地址及加密凭证不变。
     * @param id 当前访问范围内的连接标识
     * @param dataStatus 目标状态，调用方已校验合法状态码
     * @param version 页面读取的当前版本
     * @throws ApiRuntimeException 连接不存在或版本冲突
     */
    @Transactional
    public void modifyConnectionStatus(Long id, Integer dataStatus, Long version) {
        var connection = requireConnection(id, true);
        connection.setDataStatus(dataStatus);
        connection.setVersion(version);
        if (mapper.updateById(connection) != 1) {
            throw new ApiRuntimeException("连接已变化，请刷新重试");
        }
    }

    /**
     * 删除当前可访问的模型连接。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public void removeConnection(Long id) {
        requireConnection(id, true);
        mapper.deleteById(id);
    }

    /**
     * 统计模型的有效连接引用；平台管理员调用时框架自动跨租户统计。
     * @param modelId 全局模型标识
     * @return 未删除的连接数量
     */
    public long countConnectionByModel(Long modelId) {
        var query = Wrappers.<ModelConnectionEntity>lambdaQuery()
                .eq(ModelConnectionEntity::getModelId, modelId);
        return mapper.selectCount(query);
    }

    /** 凭证只写；更新留空沿用原密文，切换为none时显式清空两个存储字段。 */
    private void populateCredential(
            ModelConnectionEntity e, String key, ModelConnectionEntity old) {
        if (ModelProtocol.AUTH_NONE.equals(e.getAuthType())) {
            e.setApiKeyCiphertext(null);
            e.setCredentialKeyId(null);
            return;
        }
        if (key == null || key.isBlank()) {
            if (old == null || old.getApiKeyCiphertext() == null) {
                throw new ApiRuntimeException("请填写API Key");
            }
            e.setApiKeyCiphertext(old.getApiKeyCiphertext());
            e.setCredentialKeyId(old.getCredentialKeyId());
            return;
        }
        Long tenantId = old == null ? RequestContext.getTenantCode() : old.getTenantId();
        String ciphertext = cipher.encrypt(key, tenantId, e.getId());
        String keyId = cipher.requireKeyId();
        e.setApiKeyCiphertext(ciphertext);
        e.setCredentialKeyId(keyId);
    }

    /** 校验实际协议和认证组合；保存配置不表示已具备外部调用或验证能力。 */
    private void validateConnection(ModelConnectionEntity entity) {
        if (!entity.getProtocol().matches(ModelClientProtocol.PATTERN)
                || !entity.getAuthType().matches(ModelProtocol.AUTH_PATTERN)) {
            throw new ApiRuntimeException("不支持的协议或认证方式");
        }
        validateEndpoint(entity.getBaseUrl());
    }

    /**
     * 查询连接及模型展示信息，凭证由Controller转换器屏蔽。
     * @param id 连接器标识
     * @return 当前访问范围内的连接详情
     * @throws ApiRuntimeException 连接不存在
     */
    public ModelConnectionEntity fetchConnection(Long id) {
        var connection = mapper.selectConnectionDetail(id);
        if (connection == null) {
            throw new ApiRuntimeException("模型连接不存在");
        }
        return connection;
    }

    /** 地址只接受无用户信息、查询参数及片段的HTTP根地址。 */
    private void validateEndpoint(String value) {
        try {
            URI uri = URI.create(value);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ApiRuntimeException("模型地址必须是无凭证、查询参数及片段的HTTP地址");
        }
    }
}
