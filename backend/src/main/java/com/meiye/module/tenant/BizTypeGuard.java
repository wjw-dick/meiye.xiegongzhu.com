package com.meiye.module.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.exception.BizException;
import com.meiye.domain.entity.Tenant;
import com.meiye.domain.entity.TenantBizType;
import com.meiye.domain.mapper.TenantBizTypeMapper;
import com.meiye.domain.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BizTypeGuard {

    private final TenantBizTypeMapper tenantBizTypeMapper;
    private final TenantMapper tenantMapper;

    public void assertEnabled(String bizType) {
        if (bizType == null || bizType.isBlank()) {
            throw new BizException(400, "缺少业务品类 biz_type");
        }
        Long count = tenantBizTypeMapper.selectCount(new LambdaQueryWrapper<TenantBizType>()
                .eq(TenantBizType::getBizType, bizType)
                .eq(TenantBizType::getEnabled, 1));
        if (count == null || count == 0) {
            throw BizException.forbidden("品类【" + bizType + "】未启用，禁止该操作");
        }
    }

    public boolean isEnabled(String bizType) {
        return tenantBizTypeMapper.selectCount(new LambdaQueryWrapper<TenantBizType>()
                .eq(TenantBizType::getBizType, bizType).eq(TenantBizType::getEnabled, 1)) > 0;
    }

    public List<String> enabledCodes() {
        return tenantBizTypeMapper.selectList(new LambdaQueryWrapper<TenantBizType>()
                        .eq(TenantBizType::getEnabled, 1)).stream()
                .map(TenantBizType::getBizType).toList();
    }

    public Tenant currentTenant() {
        Long tid = com.meiye.common.context.TenantContext.getTenantId();
        Tenant t = com.meiye.common.context.TenantContext.runAs(tid, true, () -> tenantMapper.selectById(tid));
        if (t == null) {
            throw BizException.forbidden("租户不存在");
        }
        return t;
    }
}
