package com.meiye.module.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.web.BaseCrudController;
import com.meiye.domain.entity.ServiceFlowTemplate;
import com.meiye.domain.mapper.ServiceFlowTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/service-flows")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER')")
@Audit(module = "标准服务流程")
@RequiredArgsConstructor
public class ServiceFlowController extends BaseCrudController<ServiceFlowTemplate> {

    private final ServiceFlowTemplateMapper mapper;

    @Override
    protected BaseMapper<ServiceFlowTemplate> mapper() {
        return mapper;
    }
}
