package com.meiye.module.auth;

import com.meiye.common.context.LoginUser;
import com.meiye.common.crypto.AesKeyHolder;
import com.meiye.common.exception.BizException;
import com.meiye.common.security.JwtUtil;
import com.meiye.common.util.NumberGenerator;
import com.meiye.domain.entity.Customer;
import com.meiye.domain.entity.PlatformUser;
import com.meiye.domain.entity.Technician;
import com.meiye.domain.entity.Tenant;
import com.meiye.domain.entity.TenantUser;
import com.meiye.domain.mapper.CustomerMapper;
import com.meiye.domain.mapper.PlatformUserMapper;
import com.meiye.domain.mapper.TechnicianMapper;
import com.meiye.domain.mapper.TenantMapper;
import com.meiye.domain.mapper.TenantUserMapper;
import com.meiye.module.wechat.WechatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PlatformUserMapper platformUserMapper;
    private final TenantMapper tenantMapper;
    private final TenantUserMapper tenantUserMapper;
    private final CustomerMapper customerMapper;
    private final TechnicianMapper technicianMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NumberGenerator numberGenerator;
    private final WechatService wechatService;

    public Map<String, Object> platformLogin(String username, String password) {
        PlatformUser u = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getUsername, username));
        if (u == null || !passwordEncoder.matches(password, u.getPassword())) {
            throw new BizException(401, "账号或密码错误");
        }
        if (u.getStatus() == null || u.getStatus() != 1) {
            throw new BizException(403, "账号已停用");
        }
        u.setLastLoginAt(LocalDateTime.now());
        platformUserMapper.updateById(u);
        LoginUser lu = new LoginUser();
        lu.setUserId(u.getId());
        lu.setUsername(u.getUsername());
        lu.setName(u.getName());
        lu.setUserType("PLATFORM");
        lu.setRole(u.getRole());
        return result(lu, null, List.of("*"));
    }

    public Map<String, Object> tenantLogin(String tenantCode, String username, String password) {
        Tenant tenant = requireTenant(tenantCode);
        return com.meiye.common.context.TenantContext.runAs(tenant.getId(), false, () -> {
            TenantUser u = tenantUserMapper.selectOne(new LambdaQueryWrapper<TenantUser>()
                    .eq(TenantUser::getUsername, username));
            if (u == null || !passwordEncoder.matches(password, u.getPassword())) {
                throw new BizException(401, "账号或密码错误");
            }
            if (u.getStatus() == null || u.getStatus() != 1) {
                throw new BizException(403, "账号已停用");
            }
            u.setLastLoginAt(LocalDateTime.now());
            tenantUserMapper.updateById(u);
            LoginUser lu = new LoginUser();
            lu.setUserId(u.getId());
            lu.setUsername(u.getUsername());
            lu.setName(u.getName());
            lu.setUserType("TENANT");
            lu.setRole(u.getRoleCode());
            lu.setTenantId(tenant.getId());
            lu.setTenantCode(tenant.getCode());
            lu.setStoreId(u.getStoreId());
            return result(lu, tenant, permissions(u.getRoleCode()));
        });
    }

    public Map<String, Object> customerLogin(String tenantCode, String phone, String password) {
        Tenant tenant = requireTenant(tenantCode);
        return com.meiye.common.context.TenantContext.runAs(tenant.getId(), false, () -> {
            Customer c = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .and(w -> w.eq(Customer::getPhone, AesKeyHolder.encrypt(phone)).or().eq(Customer::getPhone, phone))
                    .last("limit 1"));
            if (c == null || c.getPassword() == null || !passwordEncoder.matches(password, c.getPassword())) {
                throw new BizException(401, "手机号或密码错误");
            }
            if (c.getStatus() == null || c.getStatus() != 1) {
                throw new BizException(403, "账号已禁用");
            }
            LoginUser lu = new LoginUser();
            lu.setUserId(c.getId());
            lu.setUsername(c.getPhone());
            lu.setName(c.getName());
            lu.setUserType("CUSTOMER");
            lu.setRole("CUSTOMER");
            lu.setTenantId(tenant.getId());
            lu.setTenantCode(tenant.getCode());
            lu.setCustomerId(c.getId());
            lu.setStoreId(c.getStoreId());
            return result(lu, tenant, List.of("self"));
        });
    }

    public Map<String, Object> technicianLogin(String tenantCode, String phone, String password) {
        Tenant tenant = requireTenant(tenantCode);
        return com.meiye.common.context.TenantContext.runAs(tenant.getId(), false, () -> {
            Technician t = technicianMapper.selectOne(new LambdaQueryWrapper<Technician>()
                    .and(w -> w.eq(Technician::getPhone, AesKeyHolder.encrypt(phone)).or().eq(Technician::getPhone, phone))
                    .last("limit 1"));
            if (t == null || t.getPassword() == null || !passwordEncoder.matches(password, t.getPassword())) {
                throw new BizException(401, "手机号或密码错误");
            }
            if (t.getStatus() == null || t.getStatus() != 1) {
                throw new BizException(403, "账号已停用");
            }
            LoginUser lu = new LoginUser();
            lu.setUserId(t.getId());
            lu.setUsername(t.getPhone());
            lu.setName(t.getName());
            lu.setUserType("TECHNICIAN");
            lu.setRole("TECHNICIAN");
            lu.setTenantId(tenant.getId());
            lu.setTenantCode(tenant.getCode());
            lu.setTechnicianId(t.getId());
            lu.setStoreId(t.getStoreId());
            return result(lu, tenant, List.of("worker"));
        });
    }

    @Transactional
    public Map<String, Object> wechatLogin(String tenantCode, String code, String channel, String nickname) {
        Tenant tenant = requireTenant(tenantCode);
        return com.meiye.common.context.TenantContext.runAs(tenant.getId(), false, () -> {
            WechatService.WxSession session = wechatService.code2Session(tenant.getId(), code, channel);
            Customer c = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getUnionId, session.unionId()).last("limit 1"));
            if (c == null) {
                c = new Customer();
                c.setName(nickname == null || nickname.isBlank() ? "微信用户" + session.unionId().substring(Math.max(0, session.unionId().length() - 4)) : nickname);
                c.setUnionId(session.unionId());
                c.setLevel("普通会员");
                c.setGrowth(0);
                c.setPoints(0);
                c.setDebt(java.math.BigDecimal.ZERO);
                c.setTotalConsume(java.math.BigDecimal.ZERO);
                c.setTotalRecharge(java.math.BigDecimal.ZERO);
                c.setStatus(1);
                c.setCustomerNo(numberGenerator.next("CUS"));
                customerMapper.insert(c);
            }
            LoginUser lu = new LoginUser();
            lu.setUserId(c.getId());
            lu.setUsername(c.getPhone() == null ? session.unionId() : c.getPhone());
            lu.setName(c.getName());
            lu.setUserType("CUSTOMER");
            lu.setRole("CUSTOMER");
            lu.setTenantId(tenant.getId());
            lu.setTenantCode(tenant.getCode());
            lu.setCustomerId(c.getId());
            lu.setStoreId(c.getStoreId());
            return result(lu, tenant, List.of("self"));
        });
    }

    public void changePassword(String oldPassword, String newPassword) {
        LoginUser u = com.meiye.common.context.TenantContext.user();
        if (u == null) {
            throw new BizException(401, "请先登录");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException(400, "新密码至少6位");
        }
        switch (u.getUserType()) {
            case "PLATFORM" -> {
                PlatformUser p = platformUserMapper.selectById(u.getUserId());
                if (p == null || !passwordEncoder.matches(oldPassword, p.getPassword())) {
                    throw new BizException(400, "原密码错误");
                }
                p.setPassword(passwordEncoder.encode(newPassword));
                platformUserMapper.updateById(p);
            }
            case "TENANT" -> com.meiye.common.context.TenantContext.runAs(u.getTenantId(), false, () -> {
                TenantUser t = tenantUserMapper.selectById(u.getUserId());
                if (t == null || !passwordEncoder.matches(oldPassword, t.getPassword())) {
                    throw new BizException(400, "原密码错误");
                }
                t.setPassword(passwordEncoder.encode(newPassword));
                tenantUserMapper.updateById(t);
                return null;
            });
            case "CUSTOMER" -> com.meiye.common.context.TenantContext.runAs(u.getTenantId(), false, () -> {
                Customer c = customerMapper.selectById(u.getCustomerId());
                if (c == null || c.getPassword() == null || !passwordEncoder.matches(oldPassword, c.getPassword())) {
                    throw new BizException(400, "原密码错误");
                }
                c.setPassword(passwordEncoder.encode(newPassword));
                customerMapper.updateById(c);
                return null;
            });
            case "TECHNICIAN" -> com.meiye.common.context.TenantContext.runAs(u.getTenantId(), false, () -> {
                Technician t = technicianMapper.selectById(u.getTechnicianId());
                if (t == null || t.getPassword() == null || !passwordEncoder.matches(oldPassword, t.getPassword())) {
                    throw new BizException(400, "原密码错误");
                }
                t.setPassword(passwordEncoder.encode(newPassword));
                technicianMapper.updateById(t);
                return null;
            });
            default -> throw new BizException(400, "未知用户类型");
        }
    }

    public Map<String, Object> me() {
        LoginUser u = com.meiye.common.context.TenantContext.user();
        if (u == null) {
            throw new BizException(401, "请先登录");
        }
        Tenant tenant = u.getTenantId() == null ? null : com.meiye.common.context.TenantContext.runAs(u.getTenantId(), true,
                () -> tenantMapper.selectById(u.getTenantId()));
        Map<String, Object> data = new HashMap<>();
        data.put("user", u);
        data.put("tenant", tenant);
        data.put("permissions", "PLATFORM".equals(u.getUserType()) ? List.of("*") : permissions(u.getRole()));
        return data;
    }

    private Tenant requireTenant(String tenantCode) {
        Tenant tenant = com.meiye.common.context.TenantContext.runAs(null, true, () -> tenantMapper.selectOne(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, tenantCode)));
        if (tenant == null) {
            throw new BizException(404, "租户不存在");
        }
        if ("DISABLED".equals(tenant.getStatus()) || "EXPIRED".equals(tenant.getStatus())) {
            throw new BizException(403, "租户已停用或到期");
        }
        if (tenant.getExpireAt() != null && tenant.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(403, "租户已到期");
        }
        return tenant;
    }

    private Map<String, Object> result(LoginUser u, Tenant tenant, List<String> permissions) {
        String token = jwtUtil.create(u);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", u);
        data.put("tenant", tenant);
        data.put("permissions", permissions);
        return data;
    }

    private List<String> permissions(String role) {
        return switch (role == null ? "" : role) {
            case "TENANT_ADMIN" -> List.of("*");
            case "STORE_MANAGER" -> List.of("store", "customer", "technician", "service", "appointment", "order", "card", "report");
            case "CASHIER" -> List.of("customer", "appointment", "order", "card");
            case "MARKETING" -> List.of("marketing", "customer", "report");
            case "FINANCE" -> List.of("settlement", "order", "report");
            case "TECHNICIAN" -> List.of("worker");
            default -> List.of("customer");
        };
    }
}
