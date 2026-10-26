package com.meiye.module.settlement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.Distribution;
import com.meiye.domain.entity.Order;
import com.meiye.domain.mapper.DistributionMapper;
import com.meiye.domain.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/distributions")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','MARKETING','FINANCE')")
@Audit(module = "分销佣金")
@RequiredArgsConstructor
public class DistributionController extends BaseCrudController<Distribution> {

    private final DistributionMapper mapper;
    private final OrderMapper orderMapper;

    @Override
    protected BaseMapper<Distribution> mapper() {
        return mapper;
    }

    /**
     * 两级分销：优先使用订单所属客户绑定的推荐关系（分销记录中已登记），
     * 也可通过 parentCustomerId 显式指定；一级 10%、二级 5%。
     */
    @PostMapping("/settle/{orderId}")
    @Audit(action = "生成分销佣金")
    public R<Map<String, Object>> settle(@PathVariable Long orderId,
                                         @RequestParam(required = false) Long parentCustomerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        List<Distribution> exists = mapper.selectList(new LambdaQueryWrapper<Distribution>()
                .eq(Distribution::getOrderId, orderId));
        if (!exists.isEmpty()) {
            return R.ok(Map.of("created", 0, "message", "该订单分销佣金已生成"));
        }
        BigDecimal amount = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        Long customerId = order.getCustomerId();
        Long parentId = parentCustomerId;
        if (parentId == null) {
            Distribution bound = mapper.selectOne(new LambdaQueryWrapper<Distribution>()
                    .eq(Distribution::getCustomerId, customerId)
                    .isNotNull(Distribution::getParentCustomerId)
                    .orderByDesc(Distribution::getId).last("limit 1"));
            if (bound != null) {
                parentId = bound.getParentCustomerId();
            }
        }
        int created = 0;
        if (parentId != null && customerId != null && !parentId.equals(customerId)) {
            created += insert(customerId, parentId, orderId, 1, amount, new BigDecimal("10"));
            Distribution second = mapper.selectOne(new LambdaQueryWrapper<Distribution>()
                    .eq(Distribution::getCustomerId, parentId)
                    .isNotNull(Distribution::getParentCustomerId)
                    .orderByDesc(Distribution::getId).last("limit 1"));
            if (second != null && !second.getParentCustomerId().equals(customerId)) {
                created += insert(parentId, second.getParentCustomerId(), orderId, 2, amount, new BigDecimal("5"));
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("created", created);
        if (created == 0) {
            r.put("message", "客户无推荐人，未生成佣金");
        }
        return R.ok(r);
    }

    private int insert(Long customerId, Long parentId, Long orderId, int level, BigDecimal amount, BigDecimal rate) {
        Distribution d = new Distribution();
        d.setCustomerId(customerId);
        d.setParentCustomerId(parentId);
        d.setOrderId(orderId);
        d.setLevel(level);
        d.setAmount(amount);
        d.setRate(rate);
        d.setCommission(amount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        d.setStatus("PENDING");
        mapper.insert(d);
        return 1;
    }

    @PostMapping("/{id}/settle")
    @Audit(action = "结算分销佣金")
    public R<Distribution> settleOne(@PathVariable Long id) {
        Distribution d = mapper.selectById(id);
        if (d == null) {
            throw BizException.notFound("佣金记录不存在");
        }
        if (!"PENDING".equals(d.getStatus())) {
            throw new BizException(400, "仅待结算佣金可结算");
        }
        d.setStatus("SETTLED");
        mapper.updateById(d);
        return R.ok(d);
    }

    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        Long tid = TenantContext.requireTenantId();
        List<Distribution> all = mapper.selectList(new LambdaQueryWrapper<Distribution>().eq(Distribution::getTenantId, tid));
        BigDecimal pending = all.stream().filter(d -> "PENDING".equals(d.getStatus()))
                .map(Distribution::getCommission).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal settled = all.stream().filter(d -> "SETTLED".equals(d.getStatus()))
                .map(Distribution::getCommission).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("records", all.size());
        m.put("pendingAmount", pending);
        m.put("settledAmount", settled);
        return R.ok(m);
    }
}
