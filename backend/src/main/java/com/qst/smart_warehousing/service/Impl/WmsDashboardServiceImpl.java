package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qst.smart_warehousing.VO.WmsDashboardChartsVO;
import com.qst.smart_warehousing.VO.WmsDashboardKpiVO;
import com.qst.smart_warehousing.VO.WmsDashboardOverviewVO;
import com.qst.smart_warehousing.entity.WmsAgvDevice;
import com.qst.smart_warehousing.entity.WmsAlertLog;
import com.qst.smart_warehousing.entity.WmsSortingTask;
import com.qst.smart_warehousing.entity.WmsStorageSlot;
import com.qst.smart_warehousing.mapper.WmsAgvDeviceMapper;
import com.qst.smart_warehousing.mapper.WmsAlertLogMapper;
import com.qst.smart_warehousing.mapper.WmsSortingTaskMapper;
import com.qst.smart_warehousing.mapper.WmsStorageSlotMapper;
import com.qst.smart_warehousing.service.IWmsDashboardService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class WmsDashboardServiceImpl implements IWmsDashboardService {

    @Resource
    private WmsSortingTaskMapper sortingTaskMapper;
    @Resource
    private WmsAgvDeviceMapper agvDeviceMapper;
    @Resource
    private WmsStorageSlotMapper storageSlotMapper;
    @Resource
    private WmsAlertLogMapper alertLogMapper;

    @Override
    public WmsDashboardOverviewVO getOverviewData() {
        WmsDashboardOverviewVO overview = new WmsDashboardOverviewVO();
        overview.setTimestamp(System.currentTimeMillis());

        // ================== 1. 聚合计算：KPI 核心指标卡片 ==================
        WmsDashboardKpiVO kpi = new WmsDashboardKpiVO();

        // A. 统计总任务与完成任务
        Long totalTasks = sortingTaskMapper.selectCount(null);
        Long completedTasks = sortingTaskMapper.selectCount(
                new QueryWrapper<WmsSortingTask>().eq("status", 4) // 4-已落位完工
        );
        kpi.setTotalTasks(totalTasks);
        kpi.setCompletedTasks(completedTasks);

        // 计算完工率
        if (totalTasks > 0) {
            BigDecimal compRate = BigDecimal.valueOf(completedTasks)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalTasks), 2, RoundingMode.HALF_UP);
            kpi.setCompletionRate(compRate);
        } else {
            kpi.setCompletionRate(BigDecimal.ZERO);
        }

        // B. 统计当前未处理告警数
        Long activeAlerts = alertLogMapper.selectCount(
                new QueryWrapper<WmsAlertLog>().eq("status", 0) // 0-未处理挂起
        );
        kpi.setActiveAlerts(activeAlerts);

        // C. 计算实时 AGV 利用率（工作中的 AGV / 总 AGV）
        Long totalAgvs = agvDeviceMapper.selectCount(null);
        Long workingAgvs = agvDeviceMapper.selectCount(
                new QueryWrapper<WmsAgvDevice>().eq("status", 2) // 2-任务中/忙碌
        );
        if (totalAgvs > 0) {
            BigDecimal agvUtil = BigDecimal.valueOf(workingAgvs)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalAgvs), 2, RoundingMode.HALF_UP);
            kpi.setAgvUtilizationRate(agvUtil);
        } else {
            kpi.setAgvUtilizationRate(BigDecimal.ZERO);
        }

        // D. 计算库位实时占用率
        Long totalSlots = storageSlotMapper.selectCount(null);
        Long occupiedSlots = storageSlotMapper.selectCount(
                new QueryWrapper<WmsStorageSlot>().ne("status", 0) // 非空闲
        );
        if (totalSlots > 0) {
            BigDecimal storageOccupancy = BigDecimal.valueOf(occupiedSlots)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalSlots), 2, RoundingMode.HALF_UP);
            kpi.setStorageOccupancyRate(storageOccupancy);
        } else {
            kpi.setStorageOccupancyRate(BigDecimal.ZERO);
        }

        overview.setKpi(kpi);

        // ================== 2. 聚合查询：看板图表 (Charts) ==================
        WmsDashboardChartsVO charts = new WmsDashboardChartsVO();

        // E. 任务状态分布（利用 selectMaps 进行 GROUP BY 统计，避免手写 XML）
        QueryWrapper<WmsSortingTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.select("status as name", "count(*) as value").groupBy("status");
        List<Map<String, Object>> taskStatusDist = sortingTaskMapper.selectMaps(taskWrapper);
        charts.setTaskStatusDistribution(taskStatusDist);

        // F. 告警级别分布（如 WARNING, CRITICAL 的占比分布）
        QueryWrapper<WmsAlertLog> alertWrapper = new QueryWrapper<>();
        alertWrapper.select("alert_level as name", "count(*) as value").groupBy("alert_level");
        List<Map<String, Object>> alertLevelDist = alertLogMapper.selectMaps(alertWrapper);
        charts.setAlertLevelDistribution(alertLevelDist);

        overview.setCharts(charts);

        return overview;
    }
}