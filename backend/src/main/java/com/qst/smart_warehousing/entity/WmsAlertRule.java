package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("wms_alert_rule")
public class WmsAlertRule {
    @TableId
    private Long ruleId;
    private String ruleName;
    private Integer ruleType;
    private BigDecimal threshold;
    private String alertLevel;
    private Integer isEnabled;
    private Date createTime;
}