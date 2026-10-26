package com.meiye.module.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.Customer;
import com.meiye.domain.entity.MarketingActivity;
import com.meiye.domain.mapper.CustomerMapper;
import com.meiye.domain.mapper.MarketingActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/marketing/activities")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','MARKETING')")
@Audit(module = "营销活动")
@RequiredArgsConstructor
public class MarketingActivityController extends BaseCrudController<MarketingActivity> {

    private final MarketingActivityMapper mapper;
    private final CustomerMapper customerMapper;

    @Override
    protected BaseMapper<MarketingActivity> mapper() {
        return mapper;
    }

    @GetMapping("/open/list")
    public R<List<MarketingActivity>> openList(@RequestParam(required = false) String type) {
        LocalDateTime now = LocalDateTime.now();
        return R.ok(mapper.selectList(new LambdaQueryWrapper<MarketingActivity>()
                .eq(MarketingActivity::getStatus, 1)
                .eq(type != null && !type.isBlank(), MarketingActivity::getType, type)
                .and(w -> w.isNull(MarketingActivity::getStartAt).or().le(MarketingActivity::getStartAt, now))
                .and(w -> w.isNull(MarketingActivity::getEndAt).or().ge(MarketingActivity::getEndAt, now))
                .orderByDesc(MarketingActivity::getId)));
    }

    @PostMapping("/{id}/publish")
    @Audit(action = "发布营销活动")
    public R<MarketingActivity> publish(@PathVariable Long id) {
        MarketingActivity a = require(id);
        a.setStatus(1);
        mapper.updateById(a);
        return R.ok(a);
    }

    @PostMapping("/{id}/stop")
    @Audit(action = "停止营销活动")
    public R<MarketingActivity> stop(@PathVariable Long id) {
        MarketingActivity a = require(id);
        a.setStatus(0);
        mapper.updateById(a);
        return R.ok(a);
    }

    /** 拼团/砍价参与：客户参与活动，返回活动价与参与人数（真实拼团链路由订单模块承载）。 */
    @PostMapping("/{id}/join")
    @Audit(action = "参与营销活动")
    public R<Map<String, Object>> join(@PathVariable Long id, @RequestParam Long customerId) {
        MarketingActivity a = require(id);
        Customer c = customerMapper.selectById(customerId);
        if (c == null) {
            throw BizException.notFound("会员不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activityId", a.getId());
        m.put("name", a.getName());
        m.put("price", a.getPrice());
        m.put("originPrice", a.getOriginPrice());
        m.put("customerId", customerId);
        m.put("joinedAt", LocalDateTime.now());
        m.put("status", "JOINED");
        return R.ok(m);
    }

    private MarketingActivity require(Long id) {
        MarketingActivity a = mapper.selectById(id);
        if (a == null) {
            throw BizException.notFound("活动不存在");
        }
        return a;
    }
}
