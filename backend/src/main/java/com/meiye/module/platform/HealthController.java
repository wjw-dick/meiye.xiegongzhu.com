package com.meiye.module.platform;

import com.meiye.common.web.R;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("time", LocalDateTime.now().toString());
        try {
            Integer one = jdbc.queryForObject("select 1", Integer.class);
            m.put("database", one != null && one == 1 ? "UP" : "DOWN");
        } catch (Exception e) {
            m.put("database", "DOWN");
        }
        return R.ok(m);
    }
}
