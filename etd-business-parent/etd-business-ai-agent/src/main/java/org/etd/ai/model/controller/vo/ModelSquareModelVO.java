package org.etd.ai.model.controller.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 首页平铺的已启用模型卡片，包含接入指引及当前访问范围内的连接统计。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelSquareModelVO extends ModelVO {
    /** 当前访问范围内未删除连接数量，包含停用连接；大于0表示已配置接入。 */
    private long connectionCount;
    /** 已启用连接数量，不表示真实模型调用已验证。 */
    private long enabledConnectionCount;
}
