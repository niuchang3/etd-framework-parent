package org.etd.framework.starter.storage.model.extend;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.storage.model.FileUploadModel;

import java.io.InputStream;

/**
 * InputStream 流式文件上传模型
 *
 * @author Young
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InputStreamUploadModel extends FileUploadModel<InputStream> {

    private InputStream inputStream;
    private String fileName;
    private String contentType;
    
    /**
     * 文件总字节数（若未知传入 -1，MinIO 将使用分片传输机制）
     */
    private long fileSize = -1L;

    @Override
    public InputStream getFile() {
        return inputStream;
    }
}
