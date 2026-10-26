package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_workstation", autoResultMap = true)
public class Workstation extends TenantEntity {
    private Long storeId;
    private String name;
    private String bizType;
    private String type;
    private String status;
    private String remark;
}
