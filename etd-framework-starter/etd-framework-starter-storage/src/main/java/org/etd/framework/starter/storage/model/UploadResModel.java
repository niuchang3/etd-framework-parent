package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class UploadResModel implements Serializable {

    protected String fileName;

    protected String fileUrl;

    protected Integer expired;
}
