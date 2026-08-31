package org.etd.framework.starter.storage.model.extend;

import lombok.Data;

import java.io.Serializable;

/**
 * 直传 URL 申请响应模型
 *
 * @author Young
 */
@Data
public class UploadUrlResModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 供 Web 端直接 PUT 上传文件的预签名 URL
     */
    private String uploadUrl;

    /**
     * 存储桶内最终保存的对象 Key/相对路径
     */
    private String fileName;

    /**
     * 直传链接有效时长（单位：秒）
     */
    private Integer expired;
}
