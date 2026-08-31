package org.etd.framework.starter.storage.core;

import jakarta.servlet.http.HttpServletResponse;
import org.etd.framework.starter.storage.model.UploadResModel;
import org.etd.framework.starter.storage.model.extend.ByteUploadModel;
import org.etd.framework.starter.storage.model.extend.InputStreamUploadModel;
import org.etd.framework.starter.storage.model.extend.MultipartFileUploadModel;

import java.io.OutputStream;
import java.util.List;

/**
 * 文件存储统一接口
 *
 * @author Young
 */
public interface FileUpload {

    /**
     * 针对 Web 层 MultipartFile 进行文件上传
     *
     * @param uploadModel
     * @return
     * @throws Exception
     */
    UploadResModel upload(MultipartFileUploadModel uploadModel) throws Exception;

    /**
     * 根据 InputStream 流进行文件上传
     *
     * @param uploadModel
     * @return
     * @throws Exception
     */
    UploadResModel upload(InputStreamUploadModel uploadModel) throws Exception;

    /**
     * 根据字节数组进行文件上传
     *
     * @param uploadModel
     * @return
     * @throws Exception
     */
    UploadResModel upload(ByteUploadModel uploadModel) throws Exception;

    /**
     * 删除单个文件
     *
     * @param bucketName
     * @param fileName
     * @throws Exception
     */
    void remove(String bucketName, String fileName) throws Exception;

    /**
     * 批量删除多个文件
     *
     * @param bucketName
     * @param fileNames
     * @throws Exception
     */
    void remove(String bucketName, List<String> fileNames) throws Exception;

    /**
     * 获取文件外链/访问 URL
     *
     * @param bucketName
     * @param fileName
     * @return
     * @throws Exception
     */
    String getObjectUrl(String bucketName, String fileName) throws Exception;

    /**
     * 文件下载至 HTTP Response
     *
     * @param response
     * @param bucketName
     * @param fileName
     * @throws Exception
     */
    void download(HttpServletResponse response, String bucketName, String fileName) throws Exception;

    /**
     * 文件下载至 HTTP Response（指定原始文件名）
     *
     * @param response
     * @param bucketName
     * @param fileName
     * @param originalFileName
     * @throws Exception
     */
    void download(HttpServletResponse response, String bucketName, String fileName, String originalFileName) throws Exception;

    /**
     * 文件下载写出至 OutputStream
     *
     * @param outputStream
     * @param bucketName
     * @param fileName
     * @throws Exception
     */
    void download(OutputStream outputStream, String bucketName, String fileName) throws Exception;
}
