package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 申请分片直传 URL 响应模型
 *
 * @author Young
 */
@Data
public class GeneratePartUrlResModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前分片供 Web 端直接 PUT 上传的预签名 URL
     */
    private String partUrl;

    /**
     * 当前分片序号
     */
    private Integer partNumber;

    /**
     * 直传链接有效时长（单位：秒）
     */
    private Integer expired;
}
