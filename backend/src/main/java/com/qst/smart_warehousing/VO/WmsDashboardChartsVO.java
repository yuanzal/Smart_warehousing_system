package com.qst.smart_warehousing.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Schema(name = "看板图表维度数据")
@Data
public class WmsDashboardChartsVO {
    // 1. 任务状态分布（饼图/环形图数据：key为状态名，value为数量）
    private List<Map<String, Object>> taskStatusDistribution;

    // 2. 告警级别分布（极坐标/柱状图数据：如 CRITICAL: 5个, WARNING: 12个）
    private List<Map<String, Object>> alertLevelDistribution;

    /**
     * 1. 库内存货周转时序趋势 (适配左侧折线图)
     * 推荐格式: {"xAxis": ["周一", "周二", "周三"...], "yData": [62, 68, 59...]}
     */
    private Map<String, Object> turnoverTrend;

    /**
     * 2. 智能硬件/各区域拣货效能对比 (适配右侧横向柱状图)
     * 推荐格式: {"categories": ["人工拣选", "重货B区", "冷链A区", "AGV线"], "values": [120, 180, 240, 410]}
     */
    private Map<String, Object> pickingEfficiency;
}