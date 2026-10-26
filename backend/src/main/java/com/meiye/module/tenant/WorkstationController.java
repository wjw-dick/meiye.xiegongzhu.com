package com.meiye.module.tenant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.web.BaseCrudController;
import com.meiye.domain.entity.Workstation;
import com.meiye.domain.mapper.WorkstationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/workstations")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER')")
@Audit(module = "工位管理")
@RequiredArgsConstructor
public class WorkstationController extends BaseCrudController<Workstation> {

    private final WorkstationMapper mapper;
    private final BizTypeGuard guard;

    @Override
    protected BaseMapper<Workstation> mapper() {
        return mapper;
    }

    @Override
    @PostMapping
    @Audit(action = "新增工位")
    public com.meiye.common.web.R<Workstation> save(@RequestBody Workstation body) {
        guard.assertEnabled(body.getBizType());
        mapper.insert(body);
        return com.meiye.common.web.R.ok(body);
    }
}
