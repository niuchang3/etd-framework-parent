package org.etd.framework.starter.ai.document.model;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
/**
 * 单份文档输入；资源必须可重新打开，调用方负责租户授权，不接受远程 URL。
 * @param resource 文件资源；客户端关闭自己打开的流，不删除原文件
 * @param filename 带扩展名的文件名，不包含目录
 * @param options 本次解析选项，不能为空
 */
public record DocumentParseRequest(
        Resource resource,
        String filename,
        DocumentParseOptions options) {

    /** 校验输入，文件内容不会进入自动生成的字符串表示。 */
    public DocumentParseRequest {
        Assert.notNull(resource, "文档资源不能为空");
        Assert.hasText(filename, "文档文件名不能为空");
        Assert.isTrue(!filename.contains("/") && !filename.contains("\\"), "文件名不能包含目录");
        Assert.notNull(options, "解析选项不能为空");
    }

    @Override
    public String toString() {
        return "DocumentParseRequest[文件内容已隐藏]";
    }
}
