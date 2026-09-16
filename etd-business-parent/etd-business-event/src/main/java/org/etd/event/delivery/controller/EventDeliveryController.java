package org.etd.event.delivery.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.etd.event.constant.EventPermissionCode;
import org.etd.event.delivery.controller.vo.EventDeliveryVO;
import org.etd.event.delivery.biz.EventDeliveryReplayBizService;
import org.etd.event.delivery.service.EventDeliveryService;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 事件投递任务查询和失败任务定向重播入口。
 */
@Validated
@Permission(EventPermissionCode.DELIVERY)
@RestController
@RequestMapping("/v1/event/deliveries")
public class EventDeliveryController {

    private final EventDeliveryService deliveryService;
    private final EventDeliveryReplayBizService replayBizService;

    public EventDeliveryController(EventDeliveryService deliveryService,
                                   EventDeliveryReplayBizService replayBizService) {
        this.deliveryService = deliveryService;
        this.replayBizService = replayBizService;
    }

    /**
     * 分页查询事件投递任务。
     */
    @GetMapping
    public ResultModel<IPage<EventDeliveryVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) Long subscriptionId,
            @RequestParam(required = false) @Min(0) @Max(4) Integer deliveryStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        return ResultModel.success(deliveryService.page(current, size, eventId, subscriptionId,
                deliveryStatus, startTime, endTime));
    }

    /**
     * 按事件标识精确路由物理分表并查询投递任务详情。
     */
    @GetMapping("/{eventId}/{id}")
    public ResultModel<EventDeliveryVO> get(
                                             @PathVariable @NotBlank String eventId,
                                             @PathVariable Long id) {
        return ResultModel.success(deliveryService.fetchDeliveryById(eventId, id));
    }

    /**
     * 仅重播当前失败投递任务，不会触发同一消息的其他订阅重新投递。
     */
    @AutoLog("人工重播事件投递任务")
    @PostMapping("/{eventId}/{id}/replay")
    public ResultModel<Boolean> replay(
                                        @PathVariable @NotBlank String eventId,
                                        @PathVariable Long id) {
        return ResultModel.success(replayBizService.replayDelivery(eventId, id));
    }
}
