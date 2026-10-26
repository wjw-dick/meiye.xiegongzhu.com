package com.meiye.module.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.PlatformParam;
import com.meiye.domain.mapper.PlatformParamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/platform/params")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR')")
@Audit(module = "平台参数")
@RequiredArgsConstructor
public class PlatformParamController extends BaseCrudController<PlatformParam> {

    private final PlatformParamMapper mapper;

    @Override
    protected BaseMapper<PlatformParam> mapper() {
        return mapper;
    }

    @PutMapping("/batch")
    @Audit(action = "批量保存平台参数")
    public R<Void> batch(@RequestBody Map<String, String> params) {
        params.forEach((k, v) -> {
            PlatformParam p = mapper.selectOne(new LambdaQueryWrapper<PlatformParam>().eq(PlatformParam::getParamKey, k));
            if (p != null) {
                p.setParamValue(v);
                mapper.updateById(p);
            }
        });
        return R.ok();
    }

    @GetMapping("/value/{key}")
    public R<String> value(@PathVariable String key) {
        PlatformParam p = mapper.selectOne(new LambdaQueryWrapper<PlatformParam>().eq(PlatformParam::getParamKey, key));
        return R.ok(p == null ? null : p.getParamValue());
    }
}
