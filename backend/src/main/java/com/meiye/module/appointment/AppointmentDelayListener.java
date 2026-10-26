package com.meiye.module.appointment;

import com.meiye.common.context.TenantContext;
import com.meiye.config.RabbitConfig;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentDelayListener {

    private final AppointmentService appointmentService;

    @RabbitListener(queues = RabbitConfig.APPT_RELEASE_QUEUE)
    public void onMessage(Map<String, Object> payload, Channel channel, Message message) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            Long id = Long.valueOf(String.valueOf(payload.get("appointmentId")));
            Long tid = Long.valueOf(String.valueOf(payload.get("tenantId")));
            TenantContext.runAs(tid, false, () -> appointmentService.releaseIfTimeout(id));
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("预约超时释放消息处理失败", e);
            channel.basicNack(tag, false, false);
        }
    }
}
