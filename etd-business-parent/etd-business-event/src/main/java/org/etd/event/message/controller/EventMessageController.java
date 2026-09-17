package org.etd.event.message.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.etd.event.constant.EventPermissionCode;
import org.etd.event.message.biz.EventMessageBizService;
import org.etd.event.message.controller.vo.EventMessageDetailVO;
import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.service.EventMessageService;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 持久化事件消息查询入口，消息内容不允许通过管理页面修改。
 */
@Validated
@Permission(EventPermissionCode.MESSAGE)
@RestController
@RequestMapping("/v1/event/messages")
public class EventMessageController {

    private final EventMessageService messageService;
    private final EventMessageBizService messageBizService;

    public EventMessageController(EventMessageService messageService,
                                  EventMessageBizService messageBizService) {
        this.messageService = messageService;
        this.messageBizService = messageBizService;
    }

    /**
     * 分页查询事件消息，列表不返回体积较大的上下文和业务载荷。
     *
     * @param current 当前页码，从 1 开始
     * @param size 每页数量，最大 200
     * @param eventId 事件全局唯一标识
     * @param eventType 消息携带的原始事件类型编码
     * @param eventTypeId 已解析的事件类型主键
     * @param messageStatus 消息处理状态，可选值为 {@code NORMAL}、{@code ERROR}
     * @param sourceApplication 发送事件的来源应用
     * @param startTime 接收时间范围起点，包含该时间
     * @param endTime 接收时间范围终点，不包含该时间
     * @return 事件消息分页结果
     */
    @GetMapping
    public ResultModel<IPage<EventMessageVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long eventTypeId,
            @RequestParam(required = false) EventMessageStatus messageStatus,
            @RequestParam(required = false) String sourceApplication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        return ResultModel.success(messageService.page(
                current, size, eventId, eventType, eventTypeId, messageStatus,
                sourceApplication, startTime, endTime));
    }

    /**
     * 查询消息、事件类型和全部订阅投递结果的聚合详情。
     *
     * @param eventId 事件全局唯一标识及分片键
     * @param id 消息记录主键
     * @return 消息聚合详情；异常消息没有对应类型时 {@code eventType} 为空
     */
    @GetMapping("/{eventId}/{id}")
    public ResultModel<EventMessageDetailVO> get(
            @PathVariable @NotBlank String eventId,
            @PathVariable Long id) {
        return ResultModel.success(messageBizService.fetchMessageDetail(eventId, id));
    }
}
