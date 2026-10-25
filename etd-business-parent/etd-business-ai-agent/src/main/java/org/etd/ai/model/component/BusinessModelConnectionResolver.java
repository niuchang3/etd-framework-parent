package org.etd.ai.model.component;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
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
        var connection = connections.selectById(id);
        if (connection == null || !tenantId.equals(String.valueOf(connection.getTenantId()))
                || !Objects.equals(connection.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("模型连接不存在或已停用");
        }
        var model = models.selectById(connection.getModelId());
        if (model == null || !Objects.equals(model.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("模型不存在或已停用");
        }
        var capability = Arrays.stream(ModelCapability.values())
                .filter(value -> value.getCode().equals(model.getModelType())).findFirst()
                .orElseThrow(() -> new ApiRuntimeException("模型能力不受支持"));
        // Cantor配对同时纳入目录版本和连接版本，避免任意一方更新后继续复用旧SDK。
        long sum = Math.addExact(connection.getVersion(), model.getVersion());
        long version = Math.addExact(Math.multiplyExact(sum, Math.incrementExact(sum)) / 2, model.getVersion());
        var endpoint = URI.create(connection.getBaseUrl());
        return new ModelConnectionConfiguration(tenantId, connectionId, version, true, connection.getProtocol(),
                capability, model.getModelCode(), endpoint, () -> ModelProtocol.AUTH_NONE.equals(connection.getAuthType())
                ? null : cipher.decrypt(connection.getApiKeyCiphertext(), connection.getCredentialKeyId(),
                        connection.getTenantId(), connection.getId()));
    }
}
