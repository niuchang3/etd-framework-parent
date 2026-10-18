package org.etd.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/** ModelConnectionEntity 持久化记录；租户及审计字段由框架可信上下文填充。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_connection")
public class ModelConnectionEntity extends BaseEntity {
    /** 非负乐观锁版本，修改时必须提交当前值。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    @Version
    private Long version;

    /** 租户自定义连接名称。 */
    private String connectionName;

    /** 接入协议代码。 */
    private String provider;

    /** 服务端API根地址。 */
    private String baseUrl;

    /** 认证方式。 */
    private String authType;

    /** AES-GCM认证加密后的API Key，不返回前端。 */
    @lombok.ToString.Exclude
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String apiKeyCiphertext;

    /** 环境加密主密钥标识。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String credentialKeyId;

    /** 非敏感用途备注。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
}
