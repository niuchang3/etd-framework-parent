package org.etd.ai.model.controller.vo;

import lombok.Data;

import java.time.Instant;

/** ModelConnectionVO 页面响应；所有业务ID按框架Long序列化约定传输。 */
@Data
public class ModelConnectionVO {
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

    /** 连接名称。 */
    private String connectionName;

    /** 接入协议。 */
    private String provider;

    /** 模型服务地址。 */
    private String baseUrl;

    /** 认证类型。 */
    private String authType;

    /** 是否已配置凭证，不返回明文或密文。 */
    private Boolean credentialConfigured;

    /** 用途说明。 */
    private String remark;
}
