package org.etd.upms.dict.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 字典类型保存与更新数据传输对象 DTO。
 */
@Data
public class SystemDictTypeSaveDTO {

    @NotBlank(message = "字典类型编码不能为空")
    @Size(max = 100, message = "字典类型编码不能超过100个字符")
    private String typeCode;

    @NotBlank(message = "字典类型名称不能为空")
    @Size(max = 100, message = "字典类型名称不能超过100个字符")
    private String typeName;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
