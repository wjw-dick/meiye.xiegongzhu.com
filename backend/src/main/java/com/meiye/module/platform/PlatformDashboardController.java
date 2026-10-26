package com.meiye.module.platform;

import com.meiye.common.audit.Audit;
import com.meiye.common.web.R;
import com.meiye.module.report.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/platform/dashboard")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR')")
@Audit(module = "平台数据看板")
@RequiredArgsConstructor
public class PlatformDashboardController {

    private final StatsService statsService;

    @GetMapping
    public R<Map<String, Object>> dashboard() {
        Map<String, Object> m = new HashMap<>(statsService.overview(null));
        m.put("trend", statsService.trend(null, 7));
        m.put("bizDistribution", statsService.bizDistribution());
        m.put("tenantRank", statsService.tenantRank());
        return R.ok(m);
    }

    @GetMapping("/trend")
    public R<Object> trend(@RequestParam(defaultValue = "7") int days) {
        return R.ok(statsService.trend(null, days));
    }
}
