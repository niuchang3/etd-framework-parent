package org.etd.upms.dict.controller.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 字典类型视图响应对象 VO。
 */
@Data
public class SystemDictTypeVO {

    private Long id;
    private Instant createTime;
    private Instant updateTime;
    private String typeCode;
    private String typeName;
    private Boolean builtIn;
    private Boolean enabled;
    private String remark;
}
