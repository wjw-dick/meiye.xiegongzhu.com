package com.meiye.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.meiye.common.context.TenantContext;
import com.meiye.domain.entity.TenantEntity;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Set;

@Configuration
public class MybatisPlusConfig implements MetaObjectHandler {

    /**
     * 平台级表白名单：不参与租户行级隔离。
     */
    public static final Set<String> PLATFORM_TABLES = Set.of(
            "sys_tenant", "sys_platform_user", "biz_type_plugin", "sys_global_dict",
            "platform_param", "sys_language", "sys_role"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        TenantLineInnerInterceptor tenant = new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tid = TenantContext.getTenantId();
                return new LongValue(tid == null ? 0L : tid);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return TenantContext.isIgnoreTenant() || PLATFORM_TABLES.contains(tableName.toLowerCase());
            }

            @Override
            public boolean ignoreInsert(java.util.List<net.sf.jsqlparser.schema.Column> columns, String tenantIdColumn) {
                return TenantContext.isIgnoreTenant() || columns.stream()
                        .anyMatch(c -> tenantIdColumn.equalsIgnoreCase(c.getColumnName()));
            }
        });
        interceptor.addInnerInterceptor(tenant);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Object createdAt = getFieldValByName("createdAt", metaObject);
        if (createdAt == null) {
            setFieldValByName("createdAt", now, metaObject);
        }
        setFieldValByName("updatedAt", now, metaObject);
        setFieldValByName("deleted", 0, metaObject);
        if (metaObject.getOriginalObject() instanceof TenantEntity) {
            Object tid = getFieldValByName("tenantId", metaObject);
            if (tid == null) {
                Long ctxTid = TenantContext.getTenantId();
                setFieldValByName("tenantId", ctxTid == null ? 0L : ctxTid, metaObject);
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
    }
}
