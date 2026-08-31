package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 申请生成带时效性 GET 对象访问/下载 URL 响应模型
 *
 * @author Young
 */
@Data
public class GenerateObjectUrlResModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 带有安全签名的 GET 在线预览/下载 URL
     */
    private String fileUrl;

    /**
     * 链接有效时长（单位：秒）
     */
    private Integer expired;
}
