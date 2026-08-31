package org.etd.upms.dict.controller.vo;

import lombok.Data;

import java.util.Date;

@Data
public class SystemDictDataVO {

    private Long id;
    private Date createTime;
    private Date updateTime;
    private Long dictTypeId;
    private String dictCode;
    private String dictLabel;
    private String dictValue;
    private Integer sort;
    private Boolean builtIn;
    private Boolean enabled;
    private String remark;
}
