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
@TableName(value = "t_withdrawal", autoResultMap = true)
public class Withdrawal extends TenantEntity {
    private Long techId;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal actualAmount;
    private String status;
    private String remark;
    private Long reviewedBy;
    private LocalDateTime paidAt;
}
