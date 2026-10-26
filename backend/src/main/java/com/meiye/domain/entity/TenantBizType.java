package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "tenant_biz_type", autoResultMap = true)
public class TenantBizType extends TenantEntity {
    private String bizType;
    private Integer enabled;
    private LocalDateTime enabledAt;
    private Integer inventoryEnabled;
    private String commissionOverride;
}
