package org.etd.framework.starter.storage.model.extend;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.storage.model.FileUploadModel;
import org.springframework.web.multipart.MultipartFile;

/**
 * MultipartFile 上传模型
 *
 * @author Young
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MultipartFileUploadModel extends FileUploadModel<MultipartFile> {

    private MultipartFile file;

    @Override
    public MultipartFile getFile() {
        return file;
    }
}
