package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_tenant", autoResultMap = true)
public class Tenant extends BaseEntity {
    private String code;
    private String name;
    private String contact;
    @TableField(typeHandler = AesTypeHandler.class)
    private String phone;
    private String logo;
    private Integer storeLimit;
    private Integer customerLimit;
    private Integer technicianLimit;
    private String status;
    private LocalDateTime openAt;
    private LocalDateTime expireAt;
    private String remark;
}
