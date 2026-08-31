package org.etd.framework.starter.storage.core;

import org.etd.framework.starter.storage.model.*;

/**
 * 文件存储策略统一接口
 *
 * @author Young
 */
public interface FileUpload {

    // =========================================================================
    // 1. Web 前端直传能力 (HTTP PUT 方式，不占应用服务器带宽)
    // =========================================================================

    /**
     * 生成 Web 端单文件预签名直传 URL
     */
    UploadUrlResModel generateUploadUrl(UploadUrlReqModel reqModel) throws Exception;

    /**
     * 大文件分片直传 1/3：初始化分片上传任务
     */
    InitMultipartResModel initMultipart(InitMultipartReqModel reqModel) throws Exception;

    /**
     * 大文件分片直传 2/3：生成指定分片的预签名直传 URL
     */
    GeneratePartUrlResModel generatePartUrl(GeneratePartUrlReqModel reqModel) throws Exception;

    /**
     * 大文件分片直传 3/3：完成并合并已上传的所有分片
     */
    CompleteMultipartResModel completeMultipart(CompleteMultipartReqModel reqModel) throws Exception;

    // =========================================================================
    // 2. 服务端直接上传能力 (在 JVM 内存中上传流/字节/文件)
    // =========================================================================

    /**
     * 服务端：根据 InputStream 输入流上传文件
     */
    ServerUploadResModel upload(InputStreamUploadReqModel reqModel) throws Exception;

    /**
     * 服务端：根据 byte[] 字节数组上传文件
     */
    ServerUploadResModel upload(ByteUploadReqModel reqModel) throws Exception;

    /**
     * 服务端：根据 MultipartFile 文件对象上传文件
     */
    ServerUploadResModel upload(MultipartUploadReqModel reqModel) throws Exception;
}
