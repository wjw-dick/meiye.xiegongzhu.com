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
@TableName(value = "t_settlement_item", autoResultMap = true)
public class SettlementItem extends TenantEntity {
    private Long settlementId;
    private Long technicianId;
    private Long orderId;
    private BigDecimal revenue;
    private BigDecimal rate;
    private BigDecimal commissionAmount;
}
