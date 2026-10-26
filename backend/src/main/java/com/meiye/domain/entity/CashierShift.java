package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_cashier_shift", autoResultMap = true)
public class CashierShift extends TenantEntity {
    private Long storeId;
    private Long cashierId;
    private String cashierName;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BigDecimal openingCash;
    private BigDecimal closingCash;
    private BigDecimal systemAmount;
    private BigDecimal diffAmount;
    private String status;
    private String remark;
}
