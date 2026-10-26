package com.meiye.module.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.TenantUser;
import com.meiye.domain.mapper.TenantUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/users")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN')")
@Audit(module = "租户用户")
@RequiredArgsConstructor
public class TenantUserController extends BaseCrudController<TenantUser> {

    private static final List<String> ROLES = List.of("TENANT_ADMIN", "STORE_MANAGER", "CASHIER", "TECHNICIAN", "MARKETING", "FINANCE");
    private final TenantUserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected BaseMapper<TenantUser> mapper() {
        return mapper;
    }

    @GetMapping("/roles")
    public R<List<Map<String, String>>> roles() {
        Map<String, String> names = Map.of(
                "TENANT_ADMIN", "租户管理员", "STORE_MANAGER", "门店店长", "CASHIER", "前台收银",
                "TECHNICIAN", "技师", "MARKETING", "市场运营", "FINANCE", "财务");
        return R.ok(ROLES.stream().map(r -> Map.of("code", r, "name", names.get(r))).toList());
    }

    @Override
    @PostMapping
    @Audit(action = "新增租户用户")
    public R<TenantUser> save(@RequestBody TenantUser body) {
        validate(body);
        if (mapper.selectCount(new LambdaQueryWrapper<TenantUser>().eq(TenantUser::getUsername, body.getUsername())) > 0) {
            throw new BizException(400, "用户名已存在");
        }
        body.setPassword(passwordEncoder.encode(body.getPassword() == null || body.getPassword().isBlank() ? "123456" : body.getPassword()));
        body.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        mapper.insert(body);
        return R.ok(body);
    }

    @Override
    @PutMapping
    @Audit(action = "修改租户用户")
    public R<TenantUser> update(@RequestBody TenantUser body) {
        validate(body);
        TenantUser old = mapper.selectById(body.getId());
        if (old == null) {
            throw BizException.notFound("用户不存在");
        }
        if (body.getPassword() == null || body.getPassword().isBlank()) {
            body.setPassword(old.getPassword());
        } else {
            body.setPassword(passwordEncoder.encode(body.getPassword()));
        }
        mapper.updateById(body);
        return R.ok(body);
    }

    private void validate(TenantUser body) {
        if (body.getUsername() == null || body.getUsername().isBlank()) {
            throw new BizException(400, "用户名不能为空");
        }
        if (body.getRoleCode() == null || !ROLES.contains(body.getRoleCode())) {
            throw new BizException(400, "非法角色");
        }
        if (TenantContext.user() != null && body.getId() != null
                && body.getId().equals(TenantContext.user().getUserId()) && body.getStatus() != null && body.getStatus() == 0) {
            throw new BizException(400, "不能停用当前登录账号");
        }
    }
}
