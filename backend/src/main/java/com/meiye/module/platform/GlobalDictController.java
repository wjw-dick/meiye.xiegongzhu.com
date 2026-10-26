package com.meiye.module.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.GlobalDict;
import com.meiye.domain.mapper.GlobalDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/dicts")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR')")
@Audit(module = "全局标准字典")
@RequiredArgsConstructor
public class GlobalDictController extends BaseCrudController<GlobalDict> {

    private final GlobalDictMapper mapper;

    @Override
    protected BaseMapper<GlobalDict> mapper() {
        return mapper;
    }

    @GetMapping("/type/{type}")
    public R<List<GlobalDict>> byType(@PathVariable String type) {
        return R.ok(mapper.selectList(new LambdaQueryWrapper<GlobalDict>()
                .eq(GlobalDict::getDictType, type).eq(GlobalDict::getStatus, 1).orderByAsc(GlobalDict::getSort)));
    }
}
