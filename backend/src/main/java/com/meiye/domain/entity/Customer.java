package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_customer", autoResultMap = true)
public class Customer extends TenantEntity {
    private String customerNo;
    private String name;
    @TableField(typeHandler = AesTypeHandler.class)
    private String phone;
    @TableField(typeHandler = AesTypeHandler.class)
    private String idCard;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String unionId;
    private Long storeId;
    private String level;
    private Integer growth;
    private Integer points;
    private String tags;
    private Long followerId;
    private BigDecimal debt;
    private BigDecimal totalConsume;
    private BigDecimal totalRecharge;
    private Integer status;
    private String gender;
    private LocalDate birthday;
    private String remark;
    private LocalDateTime lastVisitAt;
}
