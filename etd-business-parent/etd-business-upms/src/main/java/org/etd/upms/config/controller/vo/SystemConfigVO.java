package org.etd.upms.config.controller.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 系统参数配置视图响应对象 VO。
 */
@Data
public class SystemConfigVO {

    private Long id;
    private Instant createTime;
    private Instant updateTime;
    private String parameterKey;
    private String parameterName;
    private String parameterValue;
    private String valueType;
    private Boolean builtIn;
    private Boolean enabled;
    private String remark;
}
