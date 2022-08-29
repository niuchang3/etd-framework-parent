package org.etd.framework.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.model.NotificationMsgRequest;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.message.core.annotation.Event;
import org.etd.framework.starter.message.core.queue.extend.DefaultRabbitQueue;
import org.etd.framework.starter.message.core.service.RabbitMessageService;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Young
 * @description
 * @date 2020/12/26
 */
@Slf4j
@RestController
public class HelloController {


    @Autowired
    private RabbitMessageService rabbitMessageService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @AutoLog("Hello Controller")
    @GetMapping("/permit")
    public ResultModel hello() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 將JWT信息转换为UserBean
//		Object userDetail = UserDetailUtils.getUserDetail();
        return ResultModel.success("success");
    }


    //	@PreAuthorize("hasAnyAuthority('TEST_1') || hasAnyRole('ADMIN')")
    @AutoLog("Hello Controller")
    @GetMapping("/hello")
    public ResultModel helloController() {
        log.info("Hello Controller");
        return ResultModel.success("Hello Controller");
    }


    @AutoLog("测试")
    @GetMapping("/test")
    public void test() {
        NotificationMsgRequest msgRequest = new NotificationMsgRequest();
        msgRequest.setMessageHandleCode("defaultHandle");
        msgRequest.setMessageBody("消息主题");
        msgRequest.setRetries(1);
        msgRequest.setRequestContextModel(RequestContext.getRequestContext());
        rabbitMessageService.sendMessage(DefaultRabbitQueue.DEFAULT, msgRequest);
    }

    @Event
    @AutoLog("测试2")
    @GetMapping("/test2")
    public void test2(@RequestParam String name) {
        RList<Object> test = redissonClient.getList("test");
        test.add("测试");
    }

}
