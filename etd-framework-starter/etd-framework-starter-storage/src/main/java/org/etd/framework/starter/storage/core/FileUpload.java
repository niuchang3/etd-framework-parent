package org.etd.framework.starter.storage.core;

import org.etd.framework.starter.storage.model.UploadResModel;
import org.etd.framework.starter.storage.model.exdent.ByteUploadModel;
import org.etd.framework.starter.storage.model.exdent.InputStreamUpload;
import org.etd.framework.starter.storage.model.exdent.MultipartFileUploadModel;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;

/**
 * 文件上传你接口
 */
public interface FileUpload {
    /**
     * 针对web层进行文件上传
     *
     * @param uploadModel
     */
    UploadResModel upload(MultipartFileUploadModel uploadModel) throws Exception;

    /**
     * 根据InputStream流进行上传
     *
     * @param uploadModel
     */
    UploadResModel upload(InputStreamUpload uploadModel) throws Exception;

    /**
     * 根据字节进行上传
     *
     * @param uploadModel
     */
    UploadResModel upload(ByteUploadModel uploadModel) throws Exception;

    /**
     * 删除某个文件
     *
     * @param bucketName
     * @param fileName
     */
    void remove(String bucketName, String fileName) throws Exception;

    /**
     * 删除多个文件
     *
     * @param bucketName
     * @param fileNames
     * @throws Exception
     */
    void remove(String bucketName, List<String> fileNames) throws Exception;

    /**
     * 获取文件访问URL
     *
     * @param bucketName
     * @param fileName
     * @return
     * @throws Exception
     */
    String getObjectUrl(String bucketName, String fileName) throws Exception;

    /**
     * 文件下载
     *
     * @param response
     * @param fileName
     */
    void download(HttpServletResponse response, String bucketName, String fileName) throws Exception;


    /**
     * 文件下载
     *
     * @param response
     * @param fileName
     */
    void download(HttpServletResponse response, String bucketName, String fileName, String originalFileName) throws Exception;

    /**
     * 文件下载
     *
     * @param outputStream
     * @param bucketName
     * @param fileName
     * @throws Exception
     */
    void download(OutputStream outputStream, String bucketName, String fileName) throws Exception;
}
