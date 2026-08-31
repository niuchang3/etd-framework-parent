package org.etd.upms.dict.controller.vo;

import lombok.Data;

import java.util.Date;

@Data
public class SystemDictTypeVO {

    private Long id;
    private Date createTime;
    private Date updateTime;
    private String typeCode;
    private String typeName;
    private Boolean builtIn;
    private Boolean enabled;
    private String remark;
}
