package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("wms_alert_log")
public class WmsAlertLog {
    @TableId
    private Long alertId;
    private Long ruleId;
    private Integer alertType;
    private String alertLevel;
    private String alertContent;
    private Integer status; // 0-未处理, 1-已处理
    private Long handlerId;
    private Date createTime;
    private Date updateTime;
}