package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_global_dict", autoResultMap = true)
public class GlobalDict extends BaseEntity {
    private String dictType;
    private String label;
    private String value;
    private Integer sort;
    private Integer status;
}
