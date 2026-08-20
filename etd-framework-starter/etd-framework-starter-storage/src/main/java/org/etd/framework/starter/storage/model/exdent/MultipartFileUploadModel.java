package org.etd.framework.starter.storage.model.exdent;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.storage.model.FileUploadModel;
import org.springframework.web.multipart.MultipartFile;

/**
 * 针对于SpringMVC类型的上传
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MultipartFileUploadModel extends FileUploadModel<MultipartFile> {
    /**
     * 附件信息
     */
    @JsonIgnore
    @JSONField(serialize = false)
    protected MultipartFile file;


}
