package com.meiye.module.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.crypto.AesKeyHolder;
import com.meiye.common.exception.BizException;
import com.meiye.common.util.ExcelUtil;
import com.meiye.common.util.NumberGenerator;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.PageResult;
import com.meiye.common.web.R;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/tenant/customers")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','MARKETING')")
@Audit(module = "客户档案")
@RequiredArgsConstructor
public class CustomerController extends BaseCrudController<Customer> {

    private final CustomerMapper customerMapper;
    private final AppointmentMapper appointmentMapper;
    private final OrderMapper orderMapper;
    private final MemberCardMapper memberCardMapper;
    private final CardFlowMapper cardFlowMapper;
    private final ReviewMapper reviewMapper;
    private final FollowUpMapper followUpMapper;
    private final PointRecordMapper pointRecordMapper;
    private final NumberGenerator numberGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TenantMapper tenantMapper;

    @Override
    protected BaseMapper<Customer> mapper() {
        return customerMapper;
    }

    /** 客户数配额校验：平台开通租户时配置的 customer_limit，导入同样受控。 */
    private void assertCustomerQuota() {
        Long tid = TenantContext.requireTenantId();
        Tenant tenant = tenantMapper.selectById(tid);
        if (tenant == null || tenant.getCustomerLimit() == null) {
            return;
        }
        long count = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantId, tid));
        if (count >= tenant.getCustomerLimit()) {
            throw BizException.forbidden("客户数已达租户配额上限（" + tenant.getCustomerLimit() + "）");
        }
    }

    @Override
    @GetMapping("/page")
    public R<PageResult<Customer>> page(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String sort,
                                        @RequestParam Map<String, Object> params) {
        QueryWrapper<Customer> qw = new QueryWrapper<>();
        applyEq(qw, params, "level", "level");
        applyEq(qw, params, "status", "status");
        applyEq(qw, params, "storeId", "store_id");
        applyEq(qw, params, "followerId", "follower_id");
        applyLike(qw, params, "tags", "tags");
        Object kw = params.get("kw");
        if (kw != null && !String.valueOf(kw).isBlank()) {
            String k = String.valueOf(kw).trim();
            qw.and(w -> {
                w.like("name", k).or().like("customer_no", k);
                if (k.matches("\\d{6,}")) {
                    w.or().eq("phone", AesKeyHolder.encrypt(k)).or().eq("phone", k);
                }
            });
        }
        qw.orderByDesc("id");
        return R.ok(PageResult.of(customerMapper.selectPage(new Page<>(page, size), qw)));
    }

    @Override
    @PostMapping
    @Audit(action = "新增客户")
    public R<Customer> save(@RequestBody Customer body) {
        if (body.getName() == null || body.getName().isBlank()) {
            throw new BizException(400, "客户姓名不能为空");
        }
        checkPhone(body.getPhone(), null);
        if (body.getPhone() == null || !body.getPhone().matches("1\\d{10}")) {
            throw new BizException(400, "手机号格式不正确");
        }
        assertCustomerQuota();
        body.setCustomerNo(numberGenerator.next("CUS"));
        body.setLevel(body.getLevel() == null ? "普通会员" : body.getLevel());
        body.setGrowth(body.getGrowth() == null ? 0 : body.getGrowth());
        body.setPoints(body.getPoints() == null ? 0 : body.getPoints());
        body.setDebt(body.getDebt() == null ? BigDecimal.ZERO : body.getDebt());
        body.setTotalConsume(body.getTotalConsume() == null ? BigDecimal.ZERO : body.getTotalConsume());
        body.setTotalRecharge(body.getTotalRecharge() == null ? BigDecimal.ZERO : body.getTotalRecharge());
        body.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        if (body.getPassword() != null && !body.getPassword().isBlank()) {
            body.setPassword(passwordEncoder.encode(body.getPassword()));
        }
        customerMapper.insert(body);
        return R.ok(body);
    }

    @Override
    @PutMapping
    @Audit(action = "修改客户")
    public R<Customer> update(@RequestBody Customer body) {
        Customer old = customerMapper.selectById(body.getId());
        if (old == null) {
            throw BizException.notFound("客户不存在");
        }
        checkPhone(body.getPhone(), body.getId());
        body.setPassword(null);
        customerMapper.updateById(body);
        return R.ok(body);
    }

    @PostMapping("/{id}/disable")
    @Audit(action = "禁用客户")
    public R<Void> disable(@PathVariable Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw BizException.notFound("客户不存在");
        }
        c.setStatus(0);
        customerMapper.updateById(c);
        return R.ok();
    }

    @GetMapping("/{id}/related")
    public R<Map<String, Object>> related(@PathVariable Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw BizException.notFound("客户不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customer", c);
        m.put("appointments", appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getCustomerId, id).orderByDesc(Appointment::getId).last("limit 20")));
        m.put("orders", orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getCustomerId, id).orderByDesc(Order::getId).last("limit 20")));
        m.put("cards", memberCardMapper.selectList(new LambdaQueryWrapper<MemberCard>()
                .eq(MemberCard::getCustomerId, id).orderByDesc(MemberCard::getId)));
        m.put("cardFlows", cardFlowMapper.selectList(new LambdaQueryWrapper<CardFlow>()
                .eq(CardFlow::getCustomerId, id).orderByDesc(CardFlow::getId).last("limit 30")));
        m.put("reviews", reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getCustomerId, id).orderByDesc(Review::getId)));
        m.put("followUps", followUpMapper.selectList(new LambdaQueryWrapper<FollowUp>()
                .eq(FollowUp::getCustomerId, id).orderByDesc(FollowUp::getId)));
        m.put("points", pointRecordMapper.selectList(new LambdaQueryWrapper<PointRecord>()
                .eq(PointRecord::getCustomerId, id).orderByDesc(PointRecord::getId).last("limit 30")));
        return R.ok(m);
    }

    @PostMapping("/{id}/points")
    @Audit(action = "调整积分")
    public R<Void> adjustPoints(@PathVariable Long id, @RequestParam int points,
                                @RequestParam(defaultValue = "调整") String remark) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw BizException.notFound("客户不存在");
        }
        int after = (c.getPoints() == null ? 0 : c.getPoints()) + points;
        if (after < 0) {
            throw new BizException(400, "积分不足");
        }
        c.setPoints(after);
        customerMapper.updateById(c);
        PointRecord pr = new PointRecord();
        pr.setCustomerId(id);
        pr.setPoints(points);
        pr.setType(points >= 0 ? "EARN" : "USE");
        pr.setRemark(remark);
        pointRecordMapper.insert(pr);
        return R.ok();
    }

    @GetMapping("/export")
    @Audit(action = "导出客户")
    public void export(HttpServletResponse response) {
        List<Customer> list = customerMapper.selectList(new LambdaQueryWrapper<Customer>().orderByDesc(Customer::getId));
        List<List<Object>> rows = new ArrayList<>();
        for (Customer c : list) {
            rows.add(Arrays.asList(c.getCustomerNo(), c.getName(), c.getPhone(), c.getLevel(), c.getTags(),
                    c.getDebt(), c.getTotalConsume(), c.getTotalRecharge(), c.getStatus() == 1 ? "正常" : "禁用"));
        }
        ExcelUtil.write(response, "customers.xlsx", "客户档案",
                List.of("客户编号", "姓名", "手机号", "会员等级", "标签", "欠款", "累计消费", "累计充值", "状态"), rows);
    }

    @GetMapping("/template")
    public void template(HttpServletResponse response) {
        ExcelUtil.write(response, "customer-import-template.xlsx", "导入模板",
                List.of("姓名", "手机号", "会员等级", "标签", "备注"),
                List.of(Arrays.asList("示例客户", "13800009999", "普通会员", "新客", "Excel批量导入示例")));
    }

    @PostMapping("/import")
    @Audit(action = "导入客户")
    public R<Map<String, Object>> importCustomers(@RequestParam("file") MultipartFile file) {
        List<Map<String, String>> rows = ExcelUtil.read(file);
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                String name = row.getOrDefault("姓名", row.get("name"));
                String phone = row.getOrDefault("手机号", row.get("phone"));
                if (name == null || name.isBlank() || phone == null || !phone.matches("1\\d{10}")) {
                    throw new BizException(400, "姓名或手机号格式错误");
                }
                assertCustomerQuota();
                checkPhone(phone, null);
                Customer c = new Customer();
                c.setName(name);
                c.setPhone(phone);
                c.setLevel(blankTo(row.getOrDefault("会员等级", "普通会员"), "普通会员"));
                c.setTags(row.get("标签"));
                c.setRemark(row.get("备注"));
                c.setCustomerNo(numberGenerator.next("CUS"));
                c.setGrowth(0);
                c.setPoints(0);
                c.setDebt(BigDecimal.ZERO);
                c.setTotalConsume(BigDecimal.ZERO);
                c.setTotalRecharge(BigDecimal.ZERO);
                c.setStatus(1);
                customerMapper.insert(c);
                success++;
            } catch (Exception e) {
                errors.add("第" + (i + 2) + "行：" + e.getMessage());
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("success", success);
        r.put("failed", errors.size());
        r.put("errors", errors.stream().limit(20).toList());
        return R.ok(r);
    }

    private void checkPhone(String phone, Long excludeId) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<Customer>()
                .and(w -> w.eq(Customer::getPhone, AesKeyHolder.encrypt(phone)).or().eq(Customer::getPhone, phone));
        if (excludeId != null) {
            qw.ne(Customer::getId, excludeId);
        }
        if (customerMapper.selectCount(qw) > 0) {
            throw new BizException(400, "手机号已存在（租户内唯一）");
        }
    }

    private String blankTo(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }

    private void applyEq(QueryWrapper<Customer> qw, Map<String, Object> p, String key, String col) {
        if (p.get(key) != null && !String.valueOf(p.get(key)).isBlank()) {
            qw.eq(col, p.get(key));
        }
    }

    private void applyLike(QueryWrapper<Customer> qw, Map<String, Object> p, String key, String col) {
        if (p.get(key) != null && !String.valueOf(p.get(key)).isBlank()) {
            qw.like(col, p.get(key));
        }
    }
}
