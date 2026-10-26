package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_member_card", autoResultMap = true)
public class MemberCard extends TenantEntity {
    private String cardNo;
    private Long customerId;
    private Long cardTypeId;
    private String type;
    private String name;
    private String bizType;
    private Long storeId;
    private BigDecimal balance;
    private Integer times;
    private Integer totalTimes;
    private LocalDateTime expireAt;
    private String status;
    private Long parentId;
    private BigDecimal discount;
}
