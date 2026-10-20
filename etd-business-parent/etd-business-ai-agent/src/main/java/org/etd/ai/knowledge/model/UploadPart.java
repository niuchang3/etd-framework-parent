package org.etd.ai.knowledge.model;

/** 经对象存储核实的传输分片；不同于正文分片。
 * @param partNumber 从1开始的上传分片编号
 * @param etag 对象存储返回的分片标识
 * @param size 实际分片字节数
 */
public record UploadPart(int partNumber, String etag, long size) {}
