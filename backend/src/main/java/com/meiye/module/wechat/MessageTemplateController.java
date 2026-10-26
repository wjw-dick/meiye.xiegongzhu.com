package com.meiye.module.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.PageResult;
import com.meiye.common.web.R;
import com.meiye.domain.entity.MessageLog;
import com.meiye.domain.entity.MessageTemplate;
import com.meiye.domain.mapper.MessageLogMapper;
import com.meiye.domain.mapper.MessageTemplateMapper;
import com.meiye.module.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息模板：按租户配置模板 ID，支持全业务节点、测试发送、发送日志。
 */
@RestController
@RequestMapping("/api/tenant/message")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','MARKETING')")
@Audit(module = "消息配置")
@RequiredArgsConstructor
public class MessageTemplateController {

    private final MessageTemplateMapper templateMapper;
    private final MessageLogMapper logMapper;
    private final MessageService messageService;

    @GetMapping("/templates")
    public R<List<MessageTemplate>> templates() {
        return R.ok(templateMapper.selectList(new LambdaQueryWrapper<MessageTemplate>()
                .orderByAsc(MessageTemplate::getId)));
    }

    @PostMapping("/templates")
    @Audit(action = "新增消息模板")
    public R<MessageTemplate> create(@RequestBody MessageTemplate body) {
        body.setEnabled(body.getEnabled() == null ? 1 : body.getEnabled());
        templateMapper.insert(body);
        return R.ok(body);
    }

    @PutMapping("/templates")
    @Audit(action = "编辑消息模板")
    public R<MessageTemplate> update(@RequestBody MessageTemplate body) {
        templateMapper.updateById(body);
        return R.ok(body);
    }

    @DeleteMapping("/templates/{id}")
    @Audit(action = "删除消息模板")
    public R<Void> delete(@PathVariable Long id) {
        templateMapper.deleteById(id);
        return R.ok();
    }

    @PostMapping("/test")
    @Audit(action = "测试发送消息")
    public R<MessageLog> test(@RequestParam Long customerId,
                              @RequestParam String code,
                              @RequestParam(defaultValue = "测试消息") String title,
                              @RequestParam(defaultValue = "这是一条测试消息") String content) {
        return R.ok(messageService.send(TenantContext.requireTenantId(), customerId, code, title, content));
    }

    @GetMapping("/logs/page")
    public R<PageResult<MessageLog>> logs(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String code) {
        return R.ok(PageResult.of(logMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                new LambdaQueryWrapper<MessageLog>()
                        .eq(status != null && !status.isBlank(), MessageLog::getStatus, status)
                        .eq(code != null && !code.isBlank(), MessageLog::getCode, code)
                        .orderByDesc(MessageLog::getId))));
    }

    @PostMapping("/logs/{id}/retry")
    @Audit(action = "重试消息")
    public R<MessageLog> retry(@PathVariable Long id) {
        MessageLog log = logMapper.selectById(id);
        if (log == null) {
            return R.fail(404, "消息不存在");
        }
        messageService.deliver(log.getId(), log.getTenantId(), log.getCustomerId(),
                log.getCode(), log.getTitle(), log.getContent());
        return R.ok(logMapper.selectById(id));
    }

    @GetMapping("/codes")
    public R<Map<String, String>> codes() {
        return R.ok(Map.of(
                "APPOINTMENT_CONFIRM", "预约确认",
                "APPOINTMENT_REMIND", "预约提醒",
                "APPOINTMENT_TIMEOUT", "预约超时释放",
                "ORDER_PAID", "订单支付成功",
                "ORDER_VERIFIED", "订单核销",
                "CARD_EXPIRE", "卡项到期",
                "BALANCE_LOW", "余额不足",
                "FOLLOW_UP", "回访提醒",
                "RECHARGE", "储值到账",
                "AFTER_SALE", "售后处理"
        ));
    }
}
