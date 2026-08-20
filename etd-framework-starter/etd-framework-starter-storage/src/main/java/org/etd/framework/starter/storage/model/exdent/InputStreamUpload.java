package org.etd.framework.starter.storage.model.exdent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.storage.model.FileUploadModel;

import java.io.InputStream;


@Data
@EqualsAndHashCode(callSuper = false)
public class InputStreamUpload extends FileUploadModel<InputStream> {

    protected InputStream inputStream;

    protected String fileName;

    protected String contentType;

    @Override
    public InputStream getFile() {
        return inputStream;
    }
}
