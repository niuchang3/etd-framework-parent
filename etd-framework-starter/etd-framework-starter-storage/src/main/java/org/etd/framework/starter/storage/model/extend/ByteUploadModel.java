package org.etd.framework.starter.storage.model.extend;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.storage.model.FileUploadModel;

/**
 * 字节数组文件上传模型
 *
 * @author Young
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ByteUploadModel extends FileUploadModel<byte[]> {

    private byte[] file;
    private String fileName;
    private String contentType;

    @Override
    public byte[] getFile() {
        return file;
    }
}
