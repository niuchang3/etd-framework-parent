package org.etd.upms.config.controller.vo;

import lombok.Data;

import java.util.Date;

@Data
public class SystemConfigVO {

    private Long id;
    private Date createTime;
    private Date updateTime;
    private String parameterKey;
    private String parameterName;
    private String parameterValue;
    private String valueType;
    private Boolean builtIn;
    private Boolean enabled;
    private String remark;
}
