package org.etd.event.subscription.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.event.subscription.controller.vo.EventSubscriptionVO;
import org.etd.event.subscription.entity.EventSubscriptionEntity;

import java.util.List;

/**
 * 事件业务订阅数据访问接口。
 */
@Mapper
public interface EventSubscriptionMapper extends BaseMapper<EventSubscriptionEntity> {

    /**
     * 分页查询订阅及对应事件类型展示信息。
     */
    IPage<EventSubscriptionVO> selectSubscriptionPage(IPage<EventSubscriptionVO> page,
                                                       @Param("keyword") String keyword,
                                                       @Param("eventTypeId") Long eventTypeId,
                                                       @Param("subscriberApplication") String subscriberApplication,
                                                       @Param("dataStatus") Integer dataStatus);

    /**
     * 查询一个事件类型下仍存在的订阅数量。
     */
    long countByEventTypeId(@Param("eventTypeId") Long eventTypeId);

    /**
     * 查询启用订阅选择列表。
     */
    List<EventSubscriptionVO> selectEnabledList();

    /**
     * 按主键查询订阅及对应事件类型展示信息。
     */
    EventSubscriptionVO selectSubscriptionById(@Param("id") Long id);
}
