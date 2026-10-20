package org.etd.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

    /** 租户选中的平台模型标识，同一租户的一个模型仅允许一个未删除连接器。 */
    private Long modelId;

    /** 接入协议代码。 */
    private String protocol;

    /** 服务端API根地址。 */
    private String baseUrl;

    /** 认证方式。 */
    private String authType;

    /** AES-GCM认证加密后的API Key，不返回前端。 */
    @ToString.Exclude
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String apiKeyCiphertext;

    /** 环境加密主密钥标识。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String credentialKeyId;

    /** 平台模型名称；仅用于关联查询展示，不写入连接表。 */
    @TableField(exist = false)
    private String modelName;

    /** 模型主能力类型；仅用于关联查询展示，不写入连接表。 */
    @TableField(exist = false)
    private String modelType;

    /** 所属服务商字典值；仅用于关联查询展示，不写入连接表。 */
    @TableField(exist = false)
    private String providerCode;

    /** 模型Logo资源路径；仅用于关联查询展示，不写入连接表。 */
    @TableField(exist = false)
    private String logoPath;

}
