package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_settlement", autoResultMap = true)
public class Settlement extends TenantEntity {
    private String settlementNo;
    private String periodType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long storeId;
    private BigDecimal revenue;
    private BigDecimal techCommission;
    private BigDecimal platformShare;
    private BigDecimal storeShare;
    private BigDecimal dividend;
    private String status;
}
