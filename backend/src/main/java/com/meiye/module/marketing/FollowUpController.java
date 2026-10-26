package com.meiye.module.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.FollowUp;
import com.meiye.domain.entity.Order;
import com.meiye.domain.mapper.FollowUpMapper;
import com.meiye.domain.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/follow-ups")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','MARKETING','TECHNICIAN')")
@Audit(module = "回访管理")
@RequiredArgsConstructor
public class FollowUpController extends BaseCrudController<FollowUp> {

    private final FollowUpMapper mapper;
    private final OrderMapper orderMapper;

    @Override
    protected BaseMapper<FollowUp> mapper() {
        return mapper;
    }

    /** 消费回访：按已完成订单生成回访任务（时光机）。 */
    @PostMapping("/generate")
    @Audit(action = "生成回访任务")
    public R<Map<String, Object>> generate(@RequestParam(defaultValue = "7") int days) {
        Long tid = TenantContext.requireTenantId();
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tid)
                .in(Order::getStatus, List.of("PAID", "VERIFIED", "DONE"))
                .ge(Order::getPaidAt, from));
        int created = 0;
        for (Order o : orders) {
            Long exists = mapper.selectCount(new LambdaQueryWrapper<FollowUp>()
                    .eq(FollowUp::getOrderId, o.getId()));
            if (exists > 0) {
                continue;
            }
            FollowUp f = new FollowUp();
            f.setCustomerId(o.getCustomerId());
            f.setOrderId(o.getId());
            f.setType("CONSUME");
            f.setDueAt(LocalDateTime.now().plusDays(1));
            f.setStatus("PENDING");
            f.setContent("消费后回访：服务体验、效果与复购意向");
            mapper.insert(f);
            created++;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("created", created);
        return R.ok(m);
    }

    @PostMapping("/{id}/complete")
    @Audit(action = "完成回访")
    public R<FollowUp> complete(@PathVariable Long id, @RequestParam(required = false) String result) {
        FollowUp f = mapper.selectById(id);
        if (f == null) {
            throw BizException.notFound("回访任务不存在");
        }
        f.setStatus("DONE");
        f.setResult(result);
        f.setHandlerId(TenantContext.user() == null ? null : TenantContext.user().getUserId());
        mapper.updateById(f);
        return R.ok(f);
    }

    @GetMapping("/due")
    public R<List<FollowUp>> due() {
        return R.ok(mapper.selectList(new LambdaQueryWrapper<FollowUp>()
                .eq(FollowUp::getStatus, "PENDING")
                .le(FollowUp::getDueAt, LocalDateTime.now())
                .orderByAsc(FollowUp::getDueAt)));
    }
}
