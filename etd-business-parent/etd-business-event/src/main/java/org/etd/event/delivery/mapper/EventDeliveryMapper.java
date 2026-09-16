package org.etd.event.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.etd.event.delivery.controller.vo.EventDeliveryVO;
import org.etd.event.delivery.entity.EventDeliveryEntity;

import java.util.List;

/**
 * 事件投递任务数据访问接口，逻辑表由 ShardingSphere 路由到固定物理分表。
 */
@Mapper
public interface EventDeliveryMapper extends BaseMapper<EventDeliveryEntity> {

    /**
     * 分页查询投递任务及对应订阅展示信息。
     */
    IPage<EventDeliveryVO> selectDeliveryPage(IPage<EventDeliveryVO> page,
                                               @Param("eventId") String eventId,
                                               @Param("subscriptionId") Long subscriptionId,
                                               @Param("deliveryStatus") Integer deliveryStatus,
                                               @Param("startTime") java.time.Instant startTime,
                                               @Param("endTime") java.time.Instant endTime);

    /**
     * 查询一条事件消息面向各业务订阅的全部投递结果。
     */
    List<EventDeliveryVO> selectDeliveryListByMessageId(
            @Param("eventId") String eventId,
            @Param("eventMessageId") Long eventMessageId);
}
