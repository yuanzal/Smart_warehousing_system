package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日仓储运营统计快照表
 */
@Data
@TableName("wms_daily_report_snapshot")
public class WmsDailyReportSnapshot {

    /**
     * 快照主键
     */
    @TableId(type = IdType.AUTO)
    private Long snapshotId;

    /**
     * 统计日期(天)
     */
    private LocalDate statDate;

    /**
     * 当日总入库包裹量
     */
    private Integer totalInboundCount;

    /**
     * 当日总出库包裹量
     */
    private Integer totalOutboundCount;

    /**
     * 包裹平均分拣耗时(秒)
     */
    private Integer averageSortingDuration;

    /**
     * 货位全库整体占用率百分比
     */
    private BigDecimal slotUsageRate;

    /**
     * AGV小车当日平均稼动率(运行率)
     */
    private BigDecimal agvUtilizationRate;

    /**
     * 当日分拣异常与破损包裹总量
     */
    private Integer totalExceptionCount;

    /**
     * 项目ID
     */
    private Integer projectId;

    /**
     * 租户ID
     */
    private Integer tenantId;
}