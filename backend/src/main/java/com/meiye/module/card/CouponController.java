package com.meiye.module.card;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.Coupon;
import com.meiye.domain.mapper.CouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tenant/coupons")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','MARKETING')")
@Audit(module = "优惠券")
@RequiredArgsConstructor
public class CouponController extends BaseCrudController<Coupon> {

    private final CouponMapper mapper;

    @Override
    protected BaseMapper<Coupon> mapper() {
        return mapper;
    }

    @PostMapping("/issue")
    @Audit(action = "发放优惠券")
    public R<Coupon> issue(@RequestBody Coupon body) {
        if (body.getCustomerId() == null) {
            throw new BizException(400, "请选择会员");
        }
        body.setStatus("UNUSED");
        body.setSource(body.getSource() == null ? "ADMIN" : body.getSource());
        mapper.insert(body);
        return R.ok(body);
    }

    @PostMapping("/{id}/use")
    @Audit(action = "核销优惠券")
    public R<Coupon> use(@PathVariable Long id) {
        Coupon c = mapper.selectById(id);
        if (c == null || !"UNUSED".equals(c.getStatus())) {
            throw new BizException(400, "优惠券不可用");
        }
        c.setStatus("USED");
        mapper.updateById(c);
        return R.ok(c);
    }

    @GetMapping("/available")
    public R<List<Coupon>> available(@RequestParam Long customerId) {
        return R.ok(mapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCustomerId, customerId)
                .eq(Coupon::getStatus, "UNUSED")
                .and(w -> w.isNull(Coupon::getExpireAt).or().gt(Coupon::getExpireAt, LocalDateTime.now()))));
    }
}
