package com.meiye.module.message;

import com.rabbitmq.client.Channel;
import com.meiye.config.RabbitConfig;
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
public class NotificationListener {

    private final MessageService messageService;

    @RabbitListener(queues = RabbitConfig.NOTIFY_QUEUE)
    public void onNotify(Map<String, Object> msg, Channel channel, Message message) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            messageService.deliver(
                    Long.valueOf(String.valueOf(msg.get("logId"))),
                    Long.valueOf(String.valueOf(msg.get("tenantId"))),
                    msg.get("customerId") == null ? null : Long.valueOf(String.valueOf(msg.get("customerId"))),
                    (String) msg.get("code"),
                    (String) msg.get("title"),
                    (String) msg.get("content"));
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("通知消费失败", e);
            channel.basicNack(tag, false, false);
        }
    }
}
