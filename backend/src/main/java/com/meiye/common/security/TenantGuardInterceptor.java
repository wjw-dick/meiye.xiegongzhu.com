package com.meiye.common.security;

import com.meiye.common.context.LoginUser;
import com.meiye.common.context.TenantContext;
import com.meiye.domain.entity.AuditLog;
import com.meiye.domain.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户边界守卫：
 * 1. 平台账号访问租户接口必须先下钻（X-Tenant-Id）；
 * 2. 平台下钻为只读，任何写请求直接拒绝；
 * 3. 拒绝时返回 403 并写入审计日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantGuardInterceptor implements HandlerInterceptor {

    private final AuditLogMapper auditLogMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        LoginUser user = TenantContext.user();
        if (user != null && !moduleAllowed(path, user.getUserType())) {
            deny(request, "账号类型无权访问该模块：" + user.getUserType() + " -> " + path);
            throw new AccessDeniedException("无权访问该资源");
        }
        if (path.startsWith("/api/tenant/") && user != null && TenantContext.USER_PLATFORM.equals(user.getUserType())) {
            if (!TenantContext.isDrilldown()) {
                deny(request, "平台账号未选择下钻租户即访问租户接口");
                throw new AccessDeniedException("平台账号请先选择要下钻的租户");
            }
        }
        if (TenantContext.isDrilldown()) {
            String method = request.getMethod();
            if (!"GET".equalsIgnoreCase(method) && !"OPTIONS".equalsIgnoreCase(method)) {
                deny(request, "平台下钻租户为只读模式，禁止写操作");
                throw new AccessDeniedException("平台下钻租户为只读模式，禁止修改数据");
            }
        }
        return true;
    }

    /**
     * 模块级账号类型校验：平台只能进平台模块；租户员工只能进租户后台；
     * 客户/技师只能进各自自助端，避免拿着低权限 token 调用后台接口。
     */
    private boolean moduleAllowed(String path, String userType) {
        if (path.startsWith("/api/platform/")) {
            return TenantContext.USER_PLATFORM.equals(userType);
        }
        if (path.startsWith("/api/tenant/")) {
            return TenantContext.USER_PLATFORM.equals(userType) || TenantContext.USER_TENANT.equals(userType);
        }
        if (path.startsWith("/api/customer/")) {
            return "CUSTOMER".equals(userType);
        }
        if (path.startsWith("/api/worker/")) {
            return "TECHNICIAN".equals(userType);
        }
        return true;
    }

    /** 越权访问留痕，失败不影响主流程。 */
    private void deny(HttpServletRequest request, String reason) {
        try {
            LoginUser user = TenantContext.user();
            AuditLog log = new AuditLog();
            log.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
            log.setOperatorId(user == null ? null : user.getUserId());
            log.setOperatorName(user == null ? "匿名" : user.getName());
            log.setUserType(user == null ? "ANONYMOUS" : user.getUserType());
            log.setModule("安全");
            log.setAction("越权拦截");
            log.setTarget(request.getMethod() + " " + request.getRequestURI());
            log.setDetail(reason);
            log.setIp(clientIp(request));
            auditLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("越权审计写入失败: {}", e.getMessage());
        }
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        int comma = ip.indexOf(',');
        return comma > 0 ? ip.substring(0, comma).trim() : ip;
    }
}
