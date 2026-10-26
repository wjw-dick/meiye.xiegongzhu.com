package com.meiye.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meiye.common.context.LoginUser;
import com.meiye.common.context.TenantContext;
import com.meiye.domain.entity.AuditLog;
import com.meiye.domain.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Around("execution(* com.meiye.module..*(..)) && (@annotation(com.meiye.common.audit.Audit) || @within(com.meiye.common.audit.Audit))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable e) {
            save(pjp, e.getMessage(), System.currentTimeMillis() - start, false);
            throw e;
        }
        save(pjp, null, System.currentTimeMillis() - start, true);
        return result;
    }

    private void save(ProceedingJoinPoint pjp, String error, long cost, boolean success) {
        try {
            Audit ann = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(Audit.class);
            Audit classAnn = pjp.getTarget().getClass().getAnnotation(Audit.class);
            String module = ann != null && !ann.module().isEmpty() ? ann.module()
                    : classAnn != null ? classAnn.module() : "系统";
            String action = ann != null && !ann.action().isEmpty() ? ann.action() : pjp.getSignature().getName();
            String detail = safeJson(pjp.getArgs());
            if (!success) {
                action = action + "(失败)";
                detail = error;
            }
            AuditLog log = new AuditLog();
            LoginUser user = TenantContext.user();
            log.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
            log.setOperatorId(user == null ? null : user.getUserId());
            log.setOperatorName(user == null ? "匿名" : user.getName());
            log.setUserType(user == null ? "ANONYMOUS" : user.getUserType());
            log.setModule(module);
            log.setAction(action);
            log.setTarget(pjp.getSignature().toShortString());
            log.setDetail((detail == null ? "" : detail).length() > 2000 ? detail.substring(0, 2000) : detail);
            log.setIp(ip());
            auditLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }

    private String safeJson(Object[] args) {
        try {
            Object[] clean = Arrays.stream(args).map(a -> a instanceof MultipartFile ? "<file>" : a).toArray();
            return objectMapper.writeValueAsString(clean)
                    .replaceAll("(\"(?:password|secret|miniSecret|mpSecret|appSecret|mpAesKey)\"\\s*:\\s*\")[^\"]*(\")", "$1******$2");
        } catch (Exception e) {
            return "";
        }
    }

    private String ip() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "";
            }
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return "";
        }
    }
}
