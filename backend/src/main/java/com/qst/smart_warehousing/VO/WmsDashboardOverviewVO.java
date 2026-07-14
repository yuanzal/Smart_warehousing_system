package com.qst.smart_warehousing.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "看板统一包装")
@Data
public class WmsDashboardOverviewVO {
    private WmsDashboardKpiVO kpi;       // 顶部核心卡片指标
    private WmsDashboardChartsVO charts;  // 中部图表分析指标
    private Long timestamp;              // 服务器生成时间戳（防缓存）
}