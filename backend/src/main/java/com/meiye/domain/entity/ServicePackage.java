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
@TableName(value = "t_service_package", autoResultMap = true)
public class ServicePackage extends TenantEntity {
    private String bizType;
    private String name;
    private String type;
    private BigDecimal price;
    private BigDecimal faceValue;
    private Integer times;
    private Integer validDays;
    private String itemsJson;
    private Integer status;
    private String description;
}
