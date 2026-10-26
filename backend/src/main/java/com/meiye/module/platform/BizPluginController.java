package com.meiye.module.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.BizTypePlugin;
import com.meiye.domain.mapper.BizTypePluginMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/biz-plugins")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR')")
@Audit(module = "品类插件管理")
@RequiredArgsConstructor
public class BizPluginController extends BaseCrudController<BizTypePlugin> {

    private final BizTypePluginMapper mapper;

    @Override
    protected BaseMapper<BizTypePlugin> mapper() {
        return mapper;
    }

    @PostMapping("/{id}/toggle")
    @Audit(action = "启停品类插件")
    public R<Void> toggle(@PathVariable Long id, @RequestParam Integer status) {
        BizTypePlugin p = mapper.selectById(id);
        p.setStatus(status);
        mapper.updateById(p);
        return R.ok();
    }

    @PostMapping("/{id}/default")
    @Audit(action = "设置默认启用")
    public R<Void> defaultEnabled(@PathVariable Long id, @RequestParam Integer enabled) {
        BizTypePlugin p = mapper.selectById(id);
        p.setDefaultEnabled(enabled);
        mapper.updateById(p);
        return R.ok();
    }
}
