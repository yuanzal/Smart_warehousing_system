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
}