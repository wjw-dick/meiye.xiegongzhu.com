package com.meiye.common.security;

import com.meiye.common.context.LoginUser;
import com.meiye.common.context.TenantContext;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            String token = StringUtils.hasText(header) && header.startsWith("Bearer ") ? header.substring(7) : null;
            String tenantHeader = request.getHeader(TENANT_HEADER);
            if (token != null) {
                try {
                    LoginUser user = jwtUtil.parse(token);
                    Long tid = user.getTenantId();
                    boolean ignore = false;
                    boolean drilldown = false;
                    if (TenantContext.USER_PLATFORM.equals(user.getUserType())) {
                        if (StringUtils.hasText(tenantHeader)) {
                            tid = Long.parseLong(tenantHeader);
                            drilldown = true;
                        } else {
                            ignore = true;
                        }
                    }
                    TenantContext.set(user, tid, ignore, drilldown);
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (user.getRole() != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
                    }
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getUserType()));
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
                    auth.setDetails(user);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (ExpiredJwtException e) {
                    writeJson(response, 401, "登录已过期，请重新登录");
                    return;
                } catch (Exception e) {
                    writeJson(response, 401, "登录凭证无效");
                    return;
                }
            } else if (StringUtils.hasText(tenantHeader)) {
                try {
                    TenantContext.set(null, Long.parseLong(tenantHeader), false, false);
                } catch (NumberFormatException ignored) {
                    TenantContext.set(null, null, true, false);
                }
            } else {
                TenantContext.set(null, null, true, false);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void writeJson(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\"}");
    }
}
