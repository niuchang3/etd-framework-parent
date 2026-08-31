package org.etd.framework.starter.storage.core;

import org.etd.framework.starter.storage.model.extend.UploadUrlReqModel;
import org.etd.framework.starter.storage.model.extend.UploadUrlResModel;

/**
 * 文件存储策略统一接口
 *
 * @author Young
 */
public interface FileUpload {

    /**
     * 生成供 Web 端直传云端厂商的预签名上传 URL (HTTP PUT 方式，不经过后端服务器)
     *
     * @param reqModel 直传申请请求模型
     * @return 包含直传 URL 及存储 Key 的响应模型
     * @throws Exception
     */
    UploadUrlResModel generateUploadUrl(UploadUrlReqModel reqModel) throws Exception;
}
