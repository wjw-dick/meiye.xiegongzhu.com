package com.meiye.module.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.context.TenantContext;
import com.meiye.domain.entity.AuditLog;
import com.meiye.domain.entity.Tenant;
import com.meiye.domain.mapper.AuditLogMapper;
import com.meiye.domain.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户到期任务：每天凌晨将已过期但状态仍为 TRIAL/NORMAL 的租户置为 EXPIRED，并写审计。
 * 登录侧 AuthService 同时按 expire_at 兜底拦截，避免任务延迟期间继续登录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantExpiryJob {

    private final TenantMapper tenantMapper;
    private final AuditLogMapper auditLogMapper;

    @Scheduled(cron = "0 10 0 * * ?")
    public int autoExpire() {
        List<Tenant> list = TenantContext.runAs(null, true, () -> tenantMapper.selectList(
                new LambdaQueryWrapper<Tenant>()
                        .in(Tenant::getStatus, List.of("TRIAL", "NORMAL"))
                        .isNotNull(Tenant::getExpireAt)
                        .lt(Tenant::getExpireAt, LocalDateTime.now())));
        for (Tenant t : list) {
            t.setStatus("EXPIRED");
            TenantContext.runAs(null, true, () -> {
                tenantMapper.updateById(t);
                AuditLog log = new AuditLog();
                log.setTenantId(t.getId());
                log.setOperatorId(0L);
                log.setOperatorName("系统任务");
                log.setUserType("SYSTEM");
                log.setModule("租户管理");
                log.setAction("到期自动停用");
                log.setTarget(t.getCode() + " " + t.getName());
                log.setDetail("租户到期时间 " + t.getExpireAt() + "，已自动置为 EXPIRED");
                log.setIp("127.0.0.1");
                auditLogMapper.insert(log);
                return null;
            });
            log.info("租户 {} 已到期，自动停用", t.getCode());
        }
        return list.size();
    }
}
