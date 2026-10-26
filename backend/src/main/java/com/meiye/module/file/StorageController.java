package com.meiye.module.file;

import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.R;
import com.meiye.storage.StorageDriver;
import com.meiye.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Audit(module = "对象存储配置")
public class StorageController {

    private final StorageManager storageManager;

    @GetMapping({"/api/platform/storage", "/api/tenant/storage"})
    public R<Map<String, Object>> get(jakarta.servlet.http.HttpServletRequest request) {
        return R.ok(storageManager.maskedConfig(targetTenant(request)));
    }

    @PostMapping({"/api/platform/storage/test", "/api/tenant/storage/test"})
    public R<Map<String, Object>> test(@RequestBody Map<String, Object> body) {
        StorageDriver.StorageConf conf = new StorageDriver.StorageConf();
        conf.setDriver(String.valueOf(body.getOrDefault("driver", "LOCAL")));
        conf.setEndpoint(str(body.get("endpoint")));
        conf.setAccessKey(str(body.get("accessKey")));
        conf.setSecretKey(str(body.get("secretKey")));
        conf.setBucket(str(body.get("bucket")));
        conf.setRegion(str(body.get("region")));
        conf.setDomain(str(body.get("domain")));
        conf.setLocalDir(str(body.get("localDir")));
        conf.setPublicBase(str(body.get("publicBase")));
        boolean ok = storageManager.test(conf);
        return ok ? R.ok(Map.of("success", true, "msg", "连接测试通过")) : R.fail(400, "连接测试失败，请检查参数");
    }

    @PostMapping({"/api/platform/storage", "/api/tenant/storage"})
    @Audit(action = "保存对象存储配置")
    public R<Map<String, Object>> save(@RequestBody Map<String, Object> body,
                                       jakarta.servlet.http.HttpServletRequest request) {
        Long tid = targetTenant(request);
        String driver = String.valueOf(body.getOrDefault("driver", ""));
        if (driver.isBlank()) {
            throw new BizException(400, "请选择存储驱动");
        }
        storageManager.save(tid, driver, body);
        return R.ok(storageManager.maskedConfig(tid));
    }

    private Long targetTenant(jakarta.servlet.http.HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/platform/")) {
            return 0L;
        }
        return TenantContext.requireTenantId();
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
