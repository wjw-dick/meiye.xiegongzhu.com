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
@TableName(value = "t_debt", autoResultMap = true)
public class Debt extends TenantEntity {
    private Long customerId;
    private Long orderId;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String status;
    private String remark;
}
