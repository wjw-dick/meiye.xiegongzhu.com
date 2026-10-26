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
@TableName(value = "t_order_item", autoResultMap = true)
public class OrderItem extends TenantEntity {
    private Long orderId;
    private Long itemId;
    private Long packageId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;
    private Long technicianId;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
}
