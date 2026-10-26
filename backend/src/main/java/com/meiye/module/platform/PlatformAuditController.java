package com.meiye.module.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.web.BaseCrudController;
import com.meiye.domain.entity.AuditLog;
import com.meiye.domain.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/audit")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR')")
@RequiredArgsConstructor
public class PlatformAuditController extends BaseCrudController<AuditLog> {

    private final AuditLogMapper mapper;

    @Override
    protected BaseMapper<AuditLog> mapper() {
        return mapper;
    }
}
