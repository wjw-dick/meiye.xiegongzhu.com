package com.meiye.common.context;

import com.meiye.common.exception.BizException;

import java.util.function.Supplier;

/**
 * 租户上下文：登录后由 JWT 解析 tenant_id，ThreadLocal 传递。
 * 平台端可 ignoreTenant=true 跨租户查询；下钻租户时 drilldown=true 且只读。
 */
public final class TenantContext {

    public static final String USER_PLATFORM = "PLATFORM";
    public static final String USER_TENANT = "TENANT";
    public static final String USER_CUSTOMER = "CUSTOMER";
    public static final String USER_TECHNICIAN = "TECHNICIAN";

    private static final ThreadLocal<Ctx> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static final class Ctx {
        private Long tenantId;
        private boolean ignoreTenant;
        private boolean drilldown;
        private LoginUser user;

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public boolean isIgnoreTenant() {
            return ignoreTenant;
        }

        public void setIgnoreTenant(boolean ignoreTenant) {
            this.ignoreTenant = ignoreTenant;
        }

        public boolean isDrilldown() {
            return drilldown;
        }

        public void setDrilldown(boolean drilldown) {
            this.drilldown = drilldown;
        }

        public LoginUser getUser() {
            return user;
        }

        public void setUser(LoginUser user) {
            this.user = user;
        }
    }

    public static void set(LoginUser user, Long tenantId, boolean ignoreTenant, boolean drilldown) {
        Ctx ctx = new Ctx();
        ctx.setUser(user);
        ctx.setTenantId(tenantId);
        ctx.setIgnoreTenant(ignoreTenant);
        ctx.setDrilldown(drilldown);
        HOLDER.set(ctx);
    }

    public static Ctx get() {
        Ctx ctx = HOLDER.get();
        if (ctx == null) {
            ctx = new Ctx();
            HOLDER.set(ctx);
        }
        return ctx;
    }

    public static LoginUser user() {
        return get().getUser();
    }

    public static Long getTenantId() {
        return get().getTenantId();
    }

    public static void setTenantId(Long tenantId) {
        get().setTenantId(tenantId);
    }

    public static boolean isIgnoreTenant() {
        return get().isIgnoreTenant();
    }

    public static void setIgnoreTenant(boolean ignore) {
        get().setIgnoreTenant(ignore);
    }

    public static boolean isDrilldown() {
        return get().isDrilldown();
    }

    public static Long requireTenantId() {
        Long tid = getTenantId();
        if (tid == null) {
            throw BizException.forbidden("缺少租户上下文");
        }
        return tid;
    }

    public static String userType() {
        LoginUser u = user();
        return u == null ? null : u.getUserType();
    }

    public static boolean isPlatform() {
        return USER_PLATFORM.equals(userType());
    }

    public static <T> T runAs(Long tenantId, boolean ignore, Supplier<T> supplier) {
        Ctx old = HOLDER.get();
        try {
            set(old == null ? null : old.getUser(), tenantId, ignore, false);
            return supplier.get();
        } finally {
            if (old == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(old);
            }
        }
    }

    public static void clear() {
        HOLDER.remove();
    }
}
