package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_technician", autoResultMap = true)
public class Technician extends TenantEntity {
    private String techNo;
    private String name;
    @TableField(typeHandler = AesTypeHandler.class)
    private String phone;
    @TableField(typeHandler = AesTypeHandler.class)
    private String idCard;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private Long userId;
    private Long storeId;
    private String position;
    private String skills;
    private String certStatus;
    private String rotationStatus;
    private Integer rotationSeq;
    private BigDecimal rating;
    private Integer serviceCount;
    private BigDecimal totalRevenue;
    private Integer status;
    private String avatar;
    private LocalDate entryAt;
    private String bio;
}
