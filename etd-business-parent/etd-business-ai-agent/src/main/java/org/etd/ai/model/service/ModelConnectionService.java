package org.etd.ai.model.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.model.constant.ModelProtocol;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Objects;

/** 租户模型连接能力，不发起探测或模型调用。 */
@Service
@RequiredArgsConstructor
public class ModelConnectionService {
    private final ModelConnectionMapper mapper;
    private final ModelCredentialCipher cipher;

    /**
     * 分页查询连接，Controller仅输出脱敏VO。
     *
     * @param current 从1开始的页码
     * @param size 每页记录数或预期字节数，以方法用途为准
     * @param dataStatus 可选启停状态；不传时不过滤状态
     * @return 符合当前租户及查询条件的分页结果
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public IPage<ModelConnectionEntity> fetchPage(long current, long size, Integer dataStatus) {
        var page = new Page<ModelConnectionEntity>(current, size);
        var query =
                Wrappers.<ModelConnectionEntity>lambdaQuery()
                        .eq(dataStatus != null, ModelConnectionEntity::getDataStatus, dataStatus)
                        .orderByDesc(
                                ModelConnectionEntity::getCreateTime, ModelConnectionEntity::getId);
        return mapper.selectPage(page, query);
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
     * 创建连接并加密凭证；API Key只写不读。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @param apiKey 待加密凭证或配置值，不写入日志或普通响应
     * @return 业务记录的雪花标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public Long createConnection(ModelConnectionEntity e, String apiKey) {
        validateEndpoint(e.getBaseUrl());
        e.setId(IdWorker.getId());
        populateCredential(e, apiKey, null);
        mapper.insert(e);
        return e.getId();
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
        validateEndpoint(e.getBaseUrl());
        if ((!Objects.equals(old.getProvider(), e.getProvider())
                        || !Objects.equals(old.getBaseUrl(), e.getBaseUrl()))
                && mapper.selectBoundKnowledgeBaseCount(e.getId()) > 0) {
            throw new ApiRuntimeException("连接已被知识库使用，不能修改接入协议或模型端点");
        }
        populateCredential(e, apiKey, old);
        e.setVersion(version);
        if (mapper.updateById(e) != 1) {
            throw new ApiRuntimeException("连接已变化，请刷新重试");
        }
    }

    /**
     * 删除已校验未被模型引用的连接。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void removeConnection(Long id) {
        mapper.deleteById(id);
    }

    private void populateCredential(
            ModelConnectionEntity e, String key, ModelConnectionEntity old) {
        if (ModelProtocol.AUTH_NONE.equals(e.getAuthType())) {
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
        e.setApiKeyCiphertext(
                cipher.encrypt(
                        key,
                        old == null ? RequestContext.getTenantCode() : old.getTenantId(),
                        e.getId()));
        e.setCredentialKeyId(cipher.requireKeyId());
    }

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
