package org.etd.ai.upload.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;

/** 完成上传只接收分片信息，不允许指定桶、对象键、真实uploadId或文件摘要。 */
@Data
public class UploadCompleteDTO {
    /** 按分片序号升序排列的完整ETag清单。 */
    @NotEmpty
    @Size(max = 10000)
    private List<@Valid UploadPartDTO> parts;
}
