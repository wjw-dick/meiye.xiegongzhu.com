package com.meiye.module.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.TechServiceLog;
import com.meiye.domain.mapper.TechServiceLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant/service-logs")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','TECHNICIAN')")
@Audit(module = "服务日志")
@RequiredArgsConstructor
public class TechServiceLogController extends BaseCrudController<TechServiceLog> {

    private final TechServiceLogMapper mapper;

    @Override
    protected BaseMapper<TechServiceLog> mapper() {
        return mapper;
    }

    @GetMapping("/by-appointment/{appointmentId}")
    public R<List<TechServiceLog>> byAppointment(@PathVariable Long appointmentId) {
        return R.ok(mapper.selectList(new LambdaQueryWrapper<TechServiceLog>()
                .eq(TechServiceLog::getAppointmentId, appointmentId)
                .orderByDesc(TechServiceLog::getId)));
    }

    @GetMapping("/by-technician/{technicianId}")
    public R<List<TechServiceLog>> byTechnician(@PathVariable Long technicianId) {
        if ("TECHNICIAN".equals(TenantContext.userType())
                && TenantContext.user() != null
                && !technicianId.equals(TenantContext.user().getTechnicianId())) {
            return R.fail(403, "仅可查看本人服务日志");
        }
        return R.ok(mapper.selectList(new LambdaQueryWrapper<TechServiceLog>()
                .eq(TechServiceLog::getTechnicianId, technicianId)
                .orderByDesc(TechServiceLog::getId)));
    }
}
