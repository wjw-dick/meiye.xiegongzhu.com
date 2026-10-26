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
@TableName(value = "t_inventory_item", autoResultMap = true)
public class InventoryItem extends TenantEntity {
    private Long storeId;
    private String bizType;
    private String name;
    private String sku;
    private String unit;
    private BigDecimal stock;
    private BigDecimal costPrice;
    private BigDecimal warnStock;
    private Integer status;
}
