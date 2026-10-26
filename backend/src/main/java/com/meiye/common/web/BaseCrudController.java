package com.meiye.common.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meiye.common.audit.Audit;
import com.meiye.common.exception.BizException;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Audit
public abstract class BaseCrudController<T> {

    private static final Set<String> SKIP = Set.of("page", "size", "sort", "kw");
    private static final Map<Class<?>, Set<String>> FIELD_CACHE = new ConcurrentHashMap<>();

    protected abstract BaseMapper<T> mapper();

    @GetMapping("/page")
    public R<PageResult<T>> page(@RequestParam(defaultValue = "1") long page,
                                 @RequestParam(defaultValue = "10") long size,
                                 @RequestParam(required = false) String sort,
                                 @RequestParam Map<String, Object> params) {
        QueryWrapper<T> qw = build(params);
        if (sort != null && !sort.isBlank()) {
            String[] arr = sort.split(",");
            if (arr.length == 2 && "asc".equalsIgnoreCase(arr[1])) {
                qw.orderByAsc(camelToUnderline(arr[0]));
            } else {
                qw.orderByDesc(camelToUnderline(arr[0]));
            }
        } else {
            qw.orderByDesc("id");
        }
        Page<T> p = new Page<>(page, size);
        return R.ok(PageResult.of(mapper().selectPage(p, qw)));
    }

    @GetMapping("/list")
    public R<List<T>> list(@RequestParam Map<String, Object> params) {
        QueryWrapper<T> qw = build(params);
        qw.orderByDesc("id");
        return R.ok(mapper().selectList(qw));
    }

    @GetMapping("/{id}")
    public R<T> get(@PathVariable Long id) {
        T t = mapper().selectById(id);
        if (t == null) {
            throw BizException.notFound("数据不存在");
        }
        return R.ok(t);
    }

    @PostMapping
    public R<T> save(@RequestBody T body) {
        mapper().insert(body);
        return R.ok(body);
    }

    @PutMapping
    public R<T> update(@RequestBody T body) {
        mapper().updateById(body);
        return R.ok(body);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        mapper().deleteById(id);
        return R.ok();
    }

    @SuppressWarnings("unchecked")
    private QueryWrapper<T> build(Map<String, Object> params) {
        QueryWrapper<T> qw = new QueryWrapper<>();
        if (params == null || params.isEmpty()) {
            return qw;
        }
        Set<String> fields = fields();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if (SKIP.contains(key) || val == null || String.valueOf(val).isBlank()) {
                continue;
            }
            if (key.startsWith("like_")) {
                if (fields.contains(key.substring(5))) {
                    qw.like(camelToUnderline(key.substring(5)), val);
                }
            } else if (key.startsWith("ge_")) {
                qw.ge(camelToUnderline(key.substring(3)), val);
            } else if (key.startsWith("le_")) {
                qw.le(camelToUnderline(key.substring(3)), val);
            } else if (fields.contains(key)) {
                qw.eq(camelToUnderline(key), val);
            }
        }
        if (params.get("kw") != null && !String.valueOf(params.get("kw")).isBlank()) {
            String kw = String.valueOf(params.get("kw"));
            List<String> searchable = searchableFields(fields);
            if (!searchable.isEmpty()) {
                qw.and(w -> {
                    boolean first = true;
                    for (String f : searchable) {
                        if (first) {
                            w.like(camelToUnderline(f), kw);
                            first = false;
                        } else {
                            w.or().like(camelToUnderline(f), kw);
                        }
                    }
                });
            }
        }
        return qw;
    }

    private Set<String> fields() {
        return FIELD_CACHE.computeIfAbsent(mapper().getClass(), key -> {
            Set<String> set = new HashSet<>();
            Class<?> current = key;
            while (current != null && current != Object.class) {
                for (Class<?> itf : current.getInterfaces()) {
                    for (Type t : itf.getGenericInterfaces()) {
                        if (t instanceof ParameterizedType pt && pt.getRawType() == BaseMapper.class) {
                            Class<?> entity = (Class<?>) pt.getActualTypeArguments()[0];
                            for (Field f : entity.getDeclaredFields()) {
                                if (!Modifier.isStatic(f.getModifiers())) {
                                    set.add(f.getName());
                                }
                            }
                        }
                    }
                }
                current = current.getSuperclass();
            }
            return set;
        });
    }

    private List<String> searchableFields(Set<String> fields) {
        return List.of("name", "orderNo", "customerNo", "techNo", "appointmentNo", "cardNo", "phone", "title", "code")
                .stream().filter(fields::contains).toList();
    }

    private String camelToUnderline(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
