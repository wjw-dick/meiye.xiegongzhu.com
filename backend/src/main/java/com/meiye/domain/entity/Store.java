package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_store", autoResultMap = true)
public class Store extends TenantEntity {
    private String code;
    private String name;
    @TableField(typeHandler = AesTypeHandler.class)
    private String phone;
    private String address;
    private String serviceScope;
    private String manager;
    private String bizTypes;
    private Integer allowCrossStore;
    private Integer allowShareProfit;
    private Integer status;
    private String description;
}
