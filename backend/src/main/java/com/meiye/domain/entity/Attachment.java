package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_attachment", autoResultMap = true)
public class Attachment extends TenantEntity {
    private String bizType;
    private Long bizId;
    private String name;
    private String url;
    private Long size;
    private String mime;
    private String storageDriver;
}
