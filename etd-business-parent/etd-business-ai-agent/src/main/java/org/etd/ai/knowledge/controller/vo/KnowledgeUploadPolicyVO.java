package org.etd.ai.knowledge.controller.vo;

/** 上传技术策略，不包含前端系统参数中的扩展名筛选。
 * @param maxFileBytes 原件总大小上限，单位字节
 * @param partSizeBytes 固定上传分片大小，单位字节
 * @param sessionSeconds 续传会话有效秒数
 * @param urlSeconds 单次签名地址有效秒数
 */
public record KnowledgeUploadPolicyVO(long maxFileBytes, long partSizeBytes, long sessionSeconds, int urlSeconds) {}
