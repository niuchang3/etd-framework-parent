package org.etd.framework.starter.storage.model.exdent;

import lombok.Data;
import org.etd.framework.starter.storage.model.FileUploadModel;

import java.io.InputStream;


@Data
public class InputStreamUpload extends FileUploadModel<InputStream> {

    protected InputStream inputStream;

    protected String fileName;

    protected String contentType;

    @Override
    public InputStream getFile() {
        return inputStream;
    }
}
