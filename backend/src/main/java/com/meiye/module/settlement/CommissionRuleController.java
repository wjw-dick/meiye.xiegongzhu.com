package com.meiye.module.settlement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.BaseCrudController;
import com.meiye.domain.entity.CommissionRule;
import com.meiye.domain.mapper.CommissionRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/commission-rules")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','FINANCE')")
@Audit(module = "提成规则")
@RequiredArgsConstructor
public class CommissionRuleController extends BaseCrudController<CommissionRule> {

    private final CommissionRuleMapper mapper;

    @Override
    protected BaseMapper<CommissionRule> mapper() {
        return mapper;
    }
}
