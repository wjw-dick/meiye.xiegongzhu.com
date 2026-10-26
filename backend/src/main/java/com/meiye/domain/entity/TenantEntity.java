package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class TenantEntity extends BaseEntity {

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
