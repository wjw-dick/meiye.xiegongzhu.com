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
@TableName(value = "message_log", autoResultMap = true)
public class MessageLog extends TenantEntity {
    private Long customerId;
    private String channel;
    private String code;
    private String title;
    private String content;
    private String status;
    private String error;
    private LocalDateTime sentAt;
}
