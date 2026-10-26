package com.meiye.common.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(IPage<T> p) {
        PageResult<T> r = new PageResult<>();
        r.records = p.getRecords();
        r.total = p.getTotal();
        r.page = p.getCurrent();
        r.size = p.getSize();
        return r;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        PageResult<T> r = new PageResult<>();
        r.records = records;
        r.total = total;
        r.page = page;
        r.size = size;
        return r;
    }
}
