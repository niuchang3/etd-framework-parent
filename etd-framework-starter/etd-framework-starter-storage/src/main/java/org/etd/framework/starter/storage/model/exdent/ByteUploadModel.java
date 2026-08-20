package org.etd.framework.starter.storage.model.exdent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.storage.model.FileUploadModel;

@Data
@EqualsAndHashCode(callSuper = false)
public class ByteUploadModel extends FileUploadModel<byte[]> {
    /**
     * 文件字节信息
     */
    protected byte[] file;
    /**
     * 文件名称
     */
    protected String fileName;

    protected String contentType;

    @Override
    public byte[] getFile() {
        return file;
    }
}
