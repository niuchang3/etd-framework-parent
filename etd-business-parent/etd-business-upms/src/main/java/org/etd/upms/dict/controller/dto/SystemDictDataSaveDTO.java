package org.etd.upms.dict.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SystemDictDataSaveDTO {

    @NotNull(message = "字典类型ID不能为空")
    private Long dictTypeId;

    @NotBlank(message = "字典项编码不能为空")
    @Size(max = 100, message = "字典项编码不能超过100个字符")
    private String dictCode;

    @NotBlank(message = "字典项标签不能为空")
    @Size(max = 100, message = "字典项标签不能超过100个字符")
    private String dictLabel;

    @NotBlank(message = "字典项值不能为空")
    @Size(max = 200, message = "字典项值不能超过200个字符")
    private String dictValue;

    private Integer sort;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
