package org.etd.ai.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import org.apache.ibatis.type.JdbcType;
import org.etd.ai.model.mapper.typehandler.EmbeddingDimensionsTypeHandler;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/** 平台全局模型管理；由平台管理员维护，不存储租户地址或凭证。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_model", excludeProperty = "tenantId", autoResultMap = true)
public class ModelEntity extends BaseEntity {
    /** 乐观锁版本，新增由框架填充，更新必须携带当前值。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    @Version
    private Long version;

    /** 服务商字典值，用于筛选，不表示接入协议。 */
    private String providerCode;

    /** 平台维护的模型显示名称。 */
    private String modelName;

    /** 请求服务商时使用的模型代码，由平台维护。 */
    private String modelCode;

    /** 模型主能力类型，对应模型类型字典。 */
    private String modelType;

    /** 模型图标资源路径，不包含租户凭证。 */
    private String logoPath;

    /** 模型用途介绍。 */
    private String description;

    /** 服务商密钥获取页面，不是模型API地址。 */
    private String apiKeyUrl;

    /** 模型开通及接入指南地址。 */
    private String setupGuideUrl;

    /** 服务地址获取及填写说明，专属实例地址由租户提供。 */
    private String baseUrlHelp;

    /** 平台登记的模型支持维度集合；向量模型必填且元素为不重复的正整数，非向量模型为空；知识库可选值还须与系统支持范围取交集。 */
    @TableField(jdbcType = JdbcType.ARRAY, typeHandler = EmbeddingDimensionsTypeHandler.class)
    private Integer[] embedding;

    /** 模型管理展示顺序，数值越小越靠前。 */
    private Integer sort;
}
