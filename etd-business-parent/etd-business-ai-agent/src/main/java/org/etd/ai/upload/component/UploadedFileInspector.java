package org.etd.ai.upload.component;

import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 核对原件传输完整性并计算摘要，不执行文件类型、编码或业务大小限制。 */
@Component
public class UploadedFileInspector {
    /** 摘要计算每次读取的缓冲区字节数，不是文件大小上限。 */
    private static final int BUFFER_SIZE = 8192;

    /** 原件登记元数据；内容类型按文件名推断，不代表已经解析或验证文件格式。 */
    public record Inspection(
            /** 按文件名推断的MIME类型，未知类型使用通用二进制类型。 */
            String contentType,
            /** 对实际存储字节计算的SHA256小写摘要。 */
            String sha256) {}

    /**
     * 核对实际字节数与上传声明一致，并流式计算SHA256，不读取UPMS限制参数。
     *
     * @param source MinIO原件输入流，由调用者关闭
     * @param expected 初始化任务声明的字节数，仅用于传输完整性比对
     * @param filename 原始文件名，仅用于推断响应内容类型
     * @return 原件内容类型及后端计算的摘要
     * @throws IOException 读取原件失败时
     * @throws NoSuchAlgorithmException 运行环境无法提供SHA256算法时
     * @throws ApiRuntimeException 实际字节数与上传声明不一致时
     */
    public Inspection inspect(InputStream source, long expected, String filename)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        verifySizeAndCalculateDigest(source, expected, digest);
        String contentType = MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM).toString();
        byte[] digestBytes = digest.digest();
        String checksum = HexFormat.of().formatHex(digestBytes);
        return new Inspection(contentType, checksum);
    }

    /** 流式处理任意格式原件，提前拒绝声明大小不一致，避免登记不完整文件。 */
    private void verifySizeAndCalculateDigest(
            InputStream source, long expected, MessageDigest digest) throws IOException {
        long count = 0;
        byte[] bytes = new byte[BUFFER_SIZE];
        int read;
        while ((read = source.read(bytes)) != -1) {
            if (read > expected - count) {
                throw new ApiRuntimeException("实际文件大小与声明不一致");
            }
            count += read;
            digest.update(bytes, 0, read);
        }
        if (count != expected) {
            throw new ApiRuntimeException("实际文件大小与声明不一致");
        }
    }
}
