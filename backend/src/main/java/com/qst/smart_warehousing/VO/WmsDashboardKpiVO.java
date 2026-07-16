package com.qst.smart_warehousing.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Schema(name = "看板指标卡片数据")
@Data
public class WmsDashboardKpiVO {
    // A. 任务维度
    private Long totalTasks;          // 历史总运行分拣任务数
    private Long completedTasks;      // 已成功落位完工任务数
    private BigDecimal completionRate; // 任务完工率（百分比，例如 98.50%）

    // B. 设备与仓储维度
    private Long activeAlerts;        // 当前未处理的严重告警数
    private BigDecimal agvUtilizationRate; // 实时AGV设备利用率（工作中小车 / 总小车数）
    private BigDecimal storageOccupancyRate; // 库位实时空间占用率
}