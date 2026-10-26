package com.meiye.module.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.BaseCrudController;
import com.meiye.domain.entity.AuditLog;
import com.meiye.domain.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/audit")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN')")
@RequiredArgsConstructor
public class TenantAuditController extends BaseCrudController<AuditLog> {

    private final AuditLogMapper mapper;

    @Override
    protected BaseMapper<AuditLog> mapper() {
        return mapper;
    }
}
