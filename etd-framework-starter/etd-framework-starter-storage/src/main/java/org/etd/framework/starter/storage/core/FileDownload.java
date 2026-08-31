package org.etd.framework.starter.storage.core;

import org.etd.framework.starter.storage.model.DownloadObjectReqModel;
import org.etd.framework.starter.storage.model.GenerateObjectUrlReqModel;
import org.etd.framework.starter.storage.model.GenerateObjectUrlResModel;

import java.io.InputStream;

/**
 * 文件下载与外链生成策略接口
 *
 * @author Young
 */
public interface FileDownload {

    /**
     * 根据数据库存盘的对象 Key 重新生成带时效性的 GET 在线预览/下载 URL (供 Web/App 端前端直连访问)
     *
     * @param reqModel 包含 bucketName, fileName 及可选 expiry 的请求模型
     * @return 包含生成好的带签名外链 fileUrl 的响应模型
     * @throws Exception
     */
    GenerateObjectUrlResModel generateObjectUrl(GenerateObjectUrlReqModel reqModel) throws Exception;

    /**
     * 服务端流式下载：直接获取云端文件的 InputStream 输入流 (供后端代理下载、读取或解析)
     *
     * @param reqModel 包含 bucketName 与 fileName 的请求模型
     * @return 文件的物理 InputStream 输入流
     * @throws Exception
     */
    InputStream downloadInputStream(DownloadObjectReqModel reqModel) throws Exception;

    /**
     * 服务端物理下载：直接获取云端文件的 byte[] 字节数组
     *
     * @param reqModel 包含 bucketName 与 fileName 的请求模型
     * @return 文件的物理字节数组
     * @throws Exception
     */
    byte[] downloadBytes(DownloadObjectReqModel reqModel) throws Exception;
}
