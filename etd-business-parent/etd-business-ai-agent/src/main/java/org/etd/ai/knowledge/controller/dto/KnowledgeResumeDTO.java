package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.etd.ai.knowledge.constant.KnowledgePolicy;

/** 恢复上传时重新确认本地文件身份。 */
@Data
public class KnowledgeResumeDTO {
    /** 本地文件完整SHA-256，必须与初始化时一致。 */
    @NotBlank @Pattern(regexp = KnowledgePolicy.SHA256_PATTERN)
    private String fileSha256;
}
