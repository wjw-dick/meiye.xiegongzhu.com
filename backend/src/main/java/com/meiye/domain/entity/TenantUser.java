package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_tenant_user", autoResultMap = true)
public class TenantUser extends TenantEntity {
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String name;
    @TableField(typeHandler = AesTypeHandler.class)
    private String phone;
    private String roleCode;
    private Long storeId;
    private Integer status;
    private LocalDateTime lastLoginAt;
}
