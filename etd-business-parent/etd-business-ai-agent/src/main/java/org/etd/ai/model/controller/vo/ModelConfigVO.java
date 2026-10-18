package org.etd.ai.model.controller.vo;

import lombok.Data;

import java.time.Instant;

/** ModelConfigVO 页面响应；所有业务ID按框架Long序列化约定传输。 */
@Data
public class ModelConfigVO {
    /** 雪花业务标识。 */
    private Long id;

    /** 启停状态：0停用、1启用，对应全局BasicConstant.DataStatus。 */
    private Integer dataStatus;

    /** 更新需要回传的乐观锁版本。 */
    private Long version;

    /** 创建时间点。 */
    private Instant createTime;

    /** 更新时间点。 */
    private Instant updateTime;

    /** 连接标识。 */
    private Long connectionId;

    /** 模型显示名称。 */
    private String modelName;

    /** 模型能力类型。 */
    private String modelType;

    /** 真实模型代码。 */
    private String modelCode;

    /** 后端实测维度，当前为空，页面只读。 */
    private Integer embeddingDimensions;

    /** 本阶段固定false，不提供测试调用能力。 */
    private Boolean verified;

    /** 用途说明。 */
    private String remark;
}
