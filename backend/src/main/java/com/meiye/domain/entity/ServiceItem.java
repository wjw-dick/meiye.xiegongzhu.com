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
@TableName(value = "t_service_item", autoResultMap = true)
public class ServiceItem extends TenantEntity {
    private String bizType;
    private String name;
    private String category;
    private Integer duration;
    private BigDecimal price;
    private BigDecimal memberPrice;
    private BigDecimal commissionRate;
    private String storeIds;
    private Integer status;
    private String image;
    private String description;
    private String extJson;
}
