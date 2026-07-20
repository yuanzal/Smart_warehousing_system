package com.qst.smart_warehousing.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Schema(name = "看板指标卡片数据")
@Data
public class WmsDashboardKpiVO {
    // 1. UI 强相关的核心三大卡片指标
    private Long inboundToday;              // 当日入库落位数量（件）
    private Long outboundToday;             // 当日扣减出库数量（件）
    private BigDecimal storageOccupancyRate;// 库位实时空间占用率/饱和度（如 68.40）

    // 2. 设备健康指标
    private Long activeAlerts;              // 当前未处理的严重告警数
    private BigDecimal agvUtilizationRate;   // 实时AGV设备利用率
    private BigDecimal completionRate;       // 自动化任务完工率
}