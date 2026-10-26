package com.meiye.module.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.context.TenantContext;
import com.meiye.config.RabbitConfig;
import com.meiye.domain.entity.MessageLog;
import com.meiye.domain.entity.MessageTemplate;
import com.meiye.domain.mapper.MessageLogMapper;
import com.meiye.domain.mapper.MessageTemplateMapper;
import com.meiye.module.wechat.WechatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageLogMapper messageLogMapper;
    private final MessageTemplateMapper messageTemplateMapper;
    private final WechatService wechatService;
    private final RabbitTemplate rabbitTemplate;

    public MessageLog send(Long tenantId, Long customerId, String code, String title, String content) {
        return TenantContext.runAs(tenantId, false, () -> {
            MessageLog msgLog = new MessageLog();
            msgLog.setTenantId(tenantId);
            msgLog.setCustomerId(customerId);
            msgLog.setCode(code);
            msgLog.setTitle(title);
            msgLog.setContent(content);
            msgLog.setChannel("IN_APP");
            msgLog.setStatus("PENDING");
            messageLogMapper.insert(msgLog);
            Map<String, Object> msg = new HashMap<>();
            msg.put("logId", msgLog.getId());
            msg.put("tenantId", tenantId);
            msg.put("customerId", customerId);
            msg.put("code", code);
            msg.put("title", title);
            msg.put("content", content);
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.NOTIFY_ROUTE, msg);
            } catch (Exception e) {
                log.warn("RabbitMQ 不可用，改为同步发送: {}", e.getMessage());
                deliver(msgLog.getId(), tenantId, customerId, code, title, content);
            }
            return msgLog;
        });
    }

    public void deliver(Long logId, Long tenantId, Long customerId, String code, String title, String content) {
        boolean ok = true;
        String channel = "IN_APP";
        try {
            MessageTemplate template = TenantContext.runAs(tenantId, false, () -> messageTemplateMapper.selectOne(
                    new LambdaQueryWrapper<MessageTemplate>()
                            .eq(MessageTemplate::getCode, code)
                            .eq(MessageTemplate::getEnabled, 1)
                            .last("limit 1")));
            if (template != null && template.getTemplateId() != null) {
                channel = template.getChannel();
                ok = wechatService.sendTemplate(tenantId, customerId, template.getTemplateId(), title, content);
            }
        } catch (Exception e) {
            ok = false;
            log.error("消息发送失败 logId={}", logId, e);
        }
        final boolean sent = ok;
        final String finalChannel = channel;
        TenantContext.runAs(tenantId, false, () -> {
            MessageLog msgLog = messageLogMapper.selectById(logId);
            if (msgLog != null) {
                msgLog.setStatus(sent ? "SENT" : "FAILED");
                msgLog.setChannel(finalChannel);
                msgLog.setSentAt(LocalDateTime.now());
                if (!sent) {
                    msgLog.setError("推送通道发送失败，站内消息已兜底");
                }
                messageLogMapper.updateById(msgLog);
            }
            return null;
        });
    }
}
