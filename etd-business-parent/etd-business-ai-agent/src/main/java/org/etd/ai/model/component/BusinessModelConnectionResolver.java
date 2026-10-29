package org.etd.ai.model.component;

import java.net.URI;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.etd.ai.model.constant.ModelProtocol;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.starter.ai.model.spi.ModelConnectionResolver;
import org.springframework.stereotype.Component;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;

/** 将经过框架租户过滤的业务连接解析为原生模型配置，凭证明文只在SDK初始化边界出现。 */
@Component
@RequiredArgsConstructor
public class BusinessModelConnectionResolver implements ModelConnectionResolver {
    private final ModelConnectionMapper connections;
    private final ModelMapper models;
    private final ModelCredentialCipher cipher;

    /**
     * @param tenantId 调用方从持久化连接取得的租户标识
     * @param connectionId 已授权业务连接ID
     * @return 当前有效配置快照；每次获取SDK均重新校验启停状态
     * @throws ApiRuntimeException 连接不可访问、已停用或模型能力无效
     */
    @Override
    public ModelConnectionConfiguration resolveAuthorized(String tenantId, String connectionId) {
        // 框架SPI使用字符串标识；业务主键为bigint，必须按Long绑定，避免PostgreSQL拒绝varchar比较。
        Long id = Long.valueOf(connectionId);
        ModelConnectionEntity connection = connections.selectById(id);
        if (connection == null || !tenantId.equals(String.valueOf(connection.getTenantId()))
                || !Objects.equals(connection.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("模型连接不存在或已停用");
        }
        ModelEntity model = models.selectById(connection.getModelId());
        if (model == null || !Objects.equals(model.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("模型不存在或已停用");
        }
        ModelCapability capability = resolveCapability(model.getModelType());
        long version = calculateCacheVersion(connection.getVersion(), model.getVersion());
        URI endpoint = URI.create(connection.getBaseUrl());
        Supplier<String> apiKey = () -> decryptConnectionKey(connection);
        return new ModelConnectionConfiguration(tenantId, connectionId, version, true, connection.getProtocol(),
                capability, model.getModelCode(), endpoint, apiKey);
    }

    /** 将连接和模型目录版本合成为同一个缓存版本，任一版本变化都不继续命中原配置。
     * 沿用Cantor配对公式：先计算版本和对应的三角数，再加目录版本。
     * 加法、递增或乘法超过long范围时抛出ArithmeticException，禁止静默溢出导致缓存键冲突。
     */
    private long calculateCacheVersion(long connectionVersion, long modelVersion) {
        long versionSum = Math.addExact(connectionVersion, modelVersion);
        long nextVersionSum = Math.incrementExact(versionSum);
        long triangleNumber = Math.multiplyExact(versionSum, nextVersionSum) / 2;
        return Math.addExact(triangleNumber, modelVersion);
    }

    /** 按明确的模型类型码查找能力，不依赖枚举名称或序号。 */
    private ModelCapability resolveCapability(String modelType) {
        for (ModelCapability capability : ModelCapability.values()) {
            if (capability.getCode().equals(modelType)) {
                return capability;
            }
        }
        throw new ApiRuntimeException("模型能力不受支持");
    }

    /** 凭证仍在SDK创建时才解密，解析配置不提前读取明文。 */
    private String decryptConnectionKey(ModelConnectionEntity connection) {
        if (ModelProtocol.AUTH_NONE.equals(connection.getAuthType())) {
            return null;
        }
        return cipher.decrypt(connection.getApiKeyCiphertext(), connection.getCredentialKeyId(),
                connection.getTenantId(), connection.getId());
    }
}
