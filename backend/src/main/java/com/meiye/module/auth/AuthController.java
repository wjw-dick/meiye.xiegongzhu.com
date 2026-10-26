package com.meiye.module.auth;

import com.meiye.common.audit.Audit;
import com.meiye.common.web.R;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Audit(module = "登录认证")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @Validated LoginReq req) {
        if (req.getTenantCode() == null || req.getTenantCode().isBlank()) {
            return R.ok(authService.platformLogin(req.getUsername(), req.getPassword()));
        }
        if ("CUSTOMER".equalsIgnoreCase(req.getUserType())) {
            return R.ok(authService.customerLogin(req.getTenantCode(), req.getUsername(), req.getPassword()));
        }
        if ("TECHNICIAN".equalsIgnoreCase(req.getUserType())) {
            return R.ok(authService.technicianLogin(req.getTenantCode(), req.getUsername(), req.getPassword()));
        }
        return R.ok(authService.tenantLogin(req.getTenantCode(), req.getUsername(), req.getPassword()));
    }

    @PostMapping("/wechat/{channel}")
    public R<Map<String, Object>> wechat(@PathVariable String channel, @RequestBody WechatLoginReq req) {
        return R.ok(authService.wechatLogin(req.getTenantCode(), req.getCode(), channel.toUpperCase(), req.getNickname()));
    }

    @PostMapping("/password")
    @Audit(action = "修改密码")
    public R<Void> password(@RequestBody PasswordReq req) {
        authService.changePassword(req.getOldPassword(), req.getNewPassword());
        return R.ok();
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(authService.me());
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    @Data
    public static class LoginReq {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        private String tenantCode;
        private String userType;
    }

    @Data
    public static class WechatLoginReq {
        @NotBlank
        private String tenantCode;
        @NotBlank
        private String code;
        private String nickname;
    }

    @Data
    public static class PasswordReq {
        @NotBlank
        private String oldPassword;
        @NotBlank
        private String newPassword;
    }
}
