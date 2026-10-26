package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_commission_rule", autoResultMap = true)
public class CommissionRule extends TenantEntity {
    private String bizType;
    private String name;
    private String technicianLevel;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal rate;
    private Integer status;
}
