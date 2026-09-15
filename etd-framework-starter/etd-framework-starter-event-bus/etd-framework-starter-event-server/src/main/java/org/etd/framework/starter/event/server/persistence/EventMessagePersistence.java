package org.etd.framework.starter.event.server.persistence;

import org.etd.framework.event.core.model.EventMessage;

/**
 * 事件消息持久化端口，由承载数据库能力的业务应用实现。
 *
 * <p>Starter 只负责统一入口消费和调用编排，不依赖业务实体、Mapper 或数据库技术。
 * 实现必须保证重复接收相同 {@code eventId} 时具备幂等性，并在持久化失败时抛出异常，
 * 使消息消费链路不提交当前记录。</p>
 */
public interface EventMessagePersistence {

    /**
     * 原子持久化原始事件及其派生的待投递任务。
     *
     * @param message 已完成协议解码的统一事件消息
     * @throws Exception 持久化失败或消息不符合业务约束
     */
    void persist(EventMessage message) throws Exception;
}
