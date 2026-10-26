package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "tenant_dict", autoResultMap = true)
public class TenantDict extends TenantEntity {
    private String dictType;
    private String label;
    private String value;
    private Integer sort;
    private Integer status;
}
