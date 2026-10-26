package com.meiye.module.settlement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.PageResult;
import com.meiye.common.web.R;
import com.meiye.domain.entity.Customer;
import com.meiye.domain.entity.PointRecord;
import com.meiye.domain.mapper.CustomerMapper;
import com.meiye.domain.mapper.PointRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/points")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','MARKETING','CUSTOMER')")
@Audit(module = "会员积分")
@RequiredArgsConstructor
public class PointRecordController {

    private final PointRecordMapper mapper;
    private final CustomerMapper customerMapper;

    @GetMapping("/page")
    public R<PageResult<PointRecord>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long customerId,
                                           @RequestParam(required = false) String type) {
        var qw = new LambdaQueryWrapper<PointRecord>()
                .eq(customerId != null, PointRecord::getCustomerId, customerId)
                .eq(type != null && !type.isBlank(), PointRecord::getType, type)
                .orderByDesc(PointRecord::getId);
        return R.ok(PageResult.of(mapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), qw)));
    }

    @PostMapping("/grant")
    @Transactional
    @Audit(action = "发放积分")
    public R<PointRecord> grant(@RequestBody Map<String, Object> body) {
        Long customerId = body.get("customerId") == null ? null : Long.valueOf(String.valueOf(body.get("customerId")));
        Integer points = body.get("points") == null ? 0 : Integer.valueOf(String.valueOf(body.get("points")));
        if (customerId == null || points == 0) {
            throw new BizException(400, "请选择会员并填写非零积分");
        }
        Customer c = customerMapper.selectById(customerId);
        if (c == null) {
            throw BizException.notFound("会员不存在");
        }
        PointRecord r = new PointRecord();
        r.setCustomerId(customerId);
        r.setPoints(points);
        r.setType(points > 0 ? "EARN" : "USE");
        r.setRemark(body.get("remark") == null ? "后台调整" : String.valueOf(body.get("remark")));
        mapper.insert(r);
        c.setPoints(Math.max(0, (c.getPoints() == null ? 0 : c.getPoints()) + points));
        c.setGrowth(Math.max(0, (c.getGrowth() == null ? 0 : c.getGrowth()) + points));
        customerMapper.updateById(c);
        return R.ok(r);
    }

    @PostMapping("/exchange")
    @Transactional
    @Audit(action = "积分兑换")
    public R<Map<String, Object>> exchange(@RequestParam Long customerId, @RequestParam Integer points,
                                           @RequestParam(required = false) String item) {
        if (points == null || points <= 0) {
            throw new BizException(400, "兑换积分必须大于 0");
        }
        Customer c = customerMapper.selectById(customerId);
        if (c == null) {
            throw BizException.notFound("会员不存在");
        }
        int balance = c.getPoints() == null ? 0 : c.getPoints();
        if (balance < points) {
            throw new BizException(400, "积分不足");
        }
        PointRecord r = new PointRecord();
        r.setCustomerId(customerId);
        r.setPoints(-points);
        r.setType("USE");
        r.setRemark("积分兑换：" + (item == null ? "礼品" : item));
        mapper.insert(r);
        c.setPoints(balance - points);
        customerMapper.updateById(c);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("record", r);
        m.put("balance", c.getPoints());
        return R.ok(m);
    }
}
