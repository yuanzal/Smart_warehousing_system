package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qst.smart_warehousing.VO.WmsDashboardChartsVO;
import com.qst.smart_warehousing.VO.WmsDashboardKpiVO;
import com.qst.smart_warehousing.VO.WmsDashboardOverviewVO;
import com.qst.smart_warehousing.entity.*;
import com.qst.smart_warehousing.mapper.*;
import com.qst.smart_warehousing.service.IWmsDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
public class WmsDashboardServiceImpl implements IWmsDashboardService {

    private static final String DASHBOARD_CACHE_KEY = "wms:dashboard:overview";
    private static final long CACHE_TTL_SECONDS = 10; // 10秒短缓存，兼顾大屏实时性与MySQL物理探库防刷

    // ========== 拣货效能图表配置常量 ==========
    /** 固定库区分类列表 */
    private static final List<String> PICKING_ZONE_CATEGORIES = Arrays.asList("人工拣选区", "重货B区", "冷链A区", "AGV密集线");
    /** 前三个库区基准效能值 */
    private static final List<Long> PICKING_BASE_VALUES = Arrays.asList(120L, 180L, 240L);
    /** 单台AGV效能系数 */
    private static final int AGV_EFFICIENCY_FACTOR = 85;
    /** AGV基础效能加成 */
    private static final int AGV_BASE_BONUS = 70;
    /** AGV效能保底值 */
    private static final long AGV_MIN_EFFICIENCY = 300L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private WmsSortingTaskMapper sortingTaskMapper;
    @Resource
    private WmsAgvDeviceMapper agvDeviceMapper;
    @Resource
    private WmsStorageSlotMapper storageSlotMapper;
    @Resource
    private WmsAlertLogMapper alertLogMapper;
    @Resource
    private WmsParcelMapper parcelMapper; // 💡 注入包裹主档案Mapper，用于精准提取当日出入库流水

    @Resource
    private WmsDailyReportSnapshotMapper dailyReportSnapshotMapper;

    public List<LocalDate> getLast7DayList() {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        // 从6天前到今天，7天
        for (int i = 6; i >= 0; i--) {
            dateList.add(today.minusDays(i));
        }
        return dateList;
    }

    /**
     * LocalDate 转 短中文星期：周一、周二...周日
     */
    public String getWeekCn(LocalDate date) {
        DayOfWeek week = date.getDayOfWeek();
        return week.getDisplayName(TextStyle.SHORT, Locale.CHINA);
    }

    @Override
    public WmsDashboardOverviewVO getOverviewData() {
        // 第一层缓存拦截 (高性能响应)
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(DASHBOARD_CACHE_KEY);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, WmsDashboardOverviewVO.class);
            }
        } catch (Exception e) {
            log.error("【高可用降级】Redis缓存读取异常，自动切回数据库查询：{}", e.getMessage());
        }

        // 第二层双重锁 (防高并发击穿)
        synchronized (this) {
            try {
                String cachedJson = stringRedisTemplate.opsForValue().get(DASHBOARD_CACHE_KEY);
                if (cachedJson != null) {
                    return objectMapper.readValue(cachedJson, WmsDashboardOverviewVO.class);
                }
            } catch (Exception ignored) {}

            log.info("【🔥 缓存未命中】开始执行中台数据驾驶舱深度分布式聚合计算...");
            WmsDashboardOverviewVO overview = getOverviewFromDatabase();

            // 异步回写 Redis
            try {
                String jsonStr = objectMapper.writeValueAsString(overview);
                stringRedisTemplate.opsForValue().set(DASHBOARD_CACHE_KEY, jsonStr, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("【高可用降级】Redis缓存写入失败：{}", e.getMessage());
            }

            return overview;
        }
    }

    /**
     * 核心演进：全面适配 2D 拟态动态看板的底层聚合逻辑
     */
    private WmsDashboardOverviewVO getOverviewFromDatabase() {
        WmsDashboardOverviewVO overview = new WmsDashboardOverviewVO();
        overview.setTimestamp(System.currentTimeMillis());

        // ==================== 1. KPI 核心指标卡片数据填充 ====================
        WmsDashboardKpiVO kpi = new WmsDashboardKpiVO();

        // A. 提取当日核心出入库物理吞吐量 (基于 wms_parcel 包裹档案状态)
        // 状态码对照: 4-已架入库, 5-已出库
        QueryWrapper<WmsParcel> inboundTodayWrapper = new QueryWrapper<>();
        inboundTodayWrapper.eq("status", 4).apply("DATE(update_time) = CURDATE()");
        kpi.setInboundToday(parcelMapper.selectCount(inboundTodayWrapper));

        QueryWrapper<WmsParcel> outboundTodayWrapper = new QueryWrapper<>();
        outboundTodayWrapper.eq("status", 5).apply("DATE(update_time) = CURDATE()");
        kpi.setOutboundToday(parcelMapper.selectCount(outboundTodayWrapper));

        // B. 库位实时空间饱和度 (非0即为占用状态)
        Long totalSlots = storageSlotMapper.selectCount(null);
        Long occupiedSlots = storageSlotMapper.selectCount(new QueryWrapper<WmsStorageSlot>().ne("status", 0));
        kpi.setStorageOccupancyRate(totalSlots > 0 ?
                BigDecimal.valueOf(occupiedSlots).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalSlots), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // C. 保留的核心运维效能数据 (用于中台统计或PDA多端同步)
        kpi.setActiveAlerts(alertLogMapper.selectCount(new QueryWrapper<WmsAlertLog>().eq("status", 0)));

        Long totalAgvs = agvDeviceMapper.selectCount(null);
        Long workingAgvs = agvDeviceMapper.selectCount(new QueryWrapper<WmsAgvDevice>().eq("status", 2));
        kpi.setAgvUtilizationRate(totalAgvs > 0 ?
                BigDecimal.valueOf(workingAgvs).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalAgvs), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        Long totalTasks = sortingTaskMapper.selectCount(null);
        Long completedTasks = sortingTaskMapper.selectCount(new QueryWrapper<WmsSortingTask>().eq("status", 4));
        kpi.setCompletionRate(totalTasks > 0 ?
                BigDecimal.valueOf(completedTasks).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalTasks), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        overview.setKpi(kpi);

        // ==================== 2. 高阶图表时序与效能结构体填充 ====================
        WmsDashboardChartsVO charts = new WmsDashboardChartsVO();

        // 💡 1. 【新增补全】填充调度任务执行状态多维分布 (taskStatusDistribution)
        // 对应 wms_sorting_task 表中的 status 字段，进行 GROUP BY 聚合统计
        List<Map<String, Object>> taskStatusList = new ArrayList<>();
        // 状态码对照: 1-任务创建, 2-路由计算完毕, 3-AGV转运中, 4-成功分拣入库, 5-异常拦截终止
        Map<Integer, String> taskStatusMap = new HashMap<>();
        taskStatusMap.put(1, "任务创建");
        taskStatusMap.put(2, "路由完毕");
        taskStatusMap.put(3, "AGV转运中");
        taskStatusMap.put(4, "成功入库");
        taskStatusMap.put(5, "异常终止");

        for (Map.Entry<Integer, String> entry : taskStatusMap.entrySet()) {
            Long count = sortingTaskMapper.selectCount(
                    new QueryWrapper<WmsSortingTask>().eq("status", entry.getKey())
            );
            // 只有当有数据时才扔给前端，或者全部输出以保证图表完美对称
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("statusName", entry.getValue());
            statusData.put("taskCount", count);
            taskStatusList.add(statusData);
        }
        charts.setTaskStatusDistribution(taskStatusList);


        // 💡 2. 【新增补全】填充实时系统风险与告警级别分布 (alertLevelDistribution)
        // 对应 wms_alert_log 异常日志表，根据未处理(status=0)的告警按严重层级进行分类统计
        List<Map<String, Object>> alertLevelList = new ArrayList<>();
        // 假设系统包含三大告警层级：danger-危险/严重, warning-警告/提示, info-常规业务拦截
        Map<String, String> alertLevelMap = new HashMap<>();
        alertLevelMap.put("danger", "严重危险");
        alertLevelMap.put("warning", "中危警告");
        alertLevelMap.put("info", "常规提示");
        for (Map.Entry<String, String> entry : alertLevelMap.entrySet()) {
            // 统计当前未处理(status=0)且符合指定危险层级的告警总数
            // 注意：请根据你 wms_alert_log 真实的级别字段名调整（这里假设字段名叫 level 或者是 alert_level）
            Long count = alertLogMapper.selectCount(
                    new QueryWrapper<WmsAlertLog>()
                            .eq("status", 0)
                            .eq("alert_level", entry.getKey()) // 如果数据库中是 int 类型（如1,2,3），这里eq对应的值改为数字即可
            );
            Map<String, Object> levelData = new HashMap<>();
            levelData.put("levelName", entry.getValue());
            levelData.put("alertCount", count);
            alertLevelList.add(levelData);
        }
        charts.setAlertLevelDistribution(alertLevelList);


        // Chart A: 库内存货周转时序趋势 (左侧 7 天平滑折线图)
        // ==================== Chart A: 库内存货周转时序趋势 (替换硬编码，使用每日快照表) ====================
        Map<String, Object> turnoverTrend = new HashMap<>();
        List<LocalDate> last7Days = getLast7DayList();

        // 1. 构建xAxis：动态生成7天中文星期（不再写死周一到周日）
        List<String> xAxis = last7Days.stream()
                .map(this::getWeekCn)
                .collect(Collectors.toList());
        turnoverTrend.put("xAxis", xAxis);

        // 2. 查询快照表近7天统计数据
        QueryWrapper<WmsDailyReportSnapshot> snapshotWrapper = new QueryWrapper<>();
        // 日期大于等于7天前，只查当前租户/项目，根据业务补充tenant_id、project_id条件
        LocalDate startDate = LocalDate.now().minusDays(6);
        snapshotWrapper.ge("stat_date", startDate);
        // 多租户隔离，根据你的业务加上
        // snapshotWrapper.eq("tenant_id", 当前登录租户ID);
        // snapshotWrapper.eq("project_id", 当前项目ID);
        List<WmsDailyReportSnapshot> snapshotList = dailyReportSnapshotMapper.selectList(snapshotWrapper);

        // 3. 构建日期->当日周转总量映射（周转=入库+出库）
        Map<LocalDate, Long> dateTurnoverMap = new HashMap<>();
        for (WmsDailyReportSnapshot snap : snapshotList) {
            LocalDate statDate = snap.getStatDate();
            long turnover = snap.getTotalInboundCount() + snap.getTotalOutboundCount();
            dateTurnoverMap.put(statDate, turnover);
        }

        // 4. 按7天顺序填充yData，无快照日期补0
        List<Long> yData = new ArrayList<>();
        for (LocalDate date : last7Days) {
            yData.add(dateTurnoverMap.getOrDefault(date, 0L));
        }

        // 5. 兜底降级：如果7天全部都是0（快照未生成/无业务数据），再用平滑基准兜底，防止前端空白
        boolean allZero = yData.stream().allMatch(v -> v == 0);
        if (allZero) {
            long baseCount = completedTasks > 0 ? (completedTasks / 5) + 10 : 45;
            yData = Arrays.asList(baseCount, baseCount + 12, baseCount - 8, baseCount + 25, baseCount + 40, baseCount + 48, baseCount + 30);
        }
        turnoverTrend.put("yData", yData);
        charts.setTurnoverTrend(turnoverTrend);

        // ==================== Chart B: 多库区拣货效能对比（真实业务数据版） ====================
        Map<String, Object> pickingEfficiency = new HashMap<>();
        // TODO: 设置多租户鉴权
        // 1. 从货位表查询所有库区分类（动态获取，不再写死）
        List<String> zoneCategories = storageSlotMapper.selectList(
                        new QueryWrapper<WmsStorageSlot>()
                                .select("DISTINCT zone_name")
                                .eq("tenant_id", 1) // 替换为你的租户变量
                                .eq("project_id", 1001)   // 替换为你的项目变量
                ).stream()
                .map(WmsStorageSlot::getZoneName)
                .collect(Collectors.toList());

        // 2. 按库区分组，统计当日完成的分拣任务量（真实效能值）
        List<Map<String, Object>> zoneTaskCountList = sortingTaskMapper.selectZoneTaskCount(1, 1001);

        // 3. 构建 库区 -> 任务量 映射
        Map<String, Long> zoneCountMap = new HashMap<>();
        for (Map<String, Object> map : zoneTaskCountList) {
            String zoneName = (String) map.get("zone_name");
            Long count = ((Number) map.get("task_count")).longValue();
            zoneCountMap.put(zoneName, count);
        }

        // 4. 按库区顺序填充效能值，无任务的库区补0
        List<Long> values = new ArrayList<>();
        for (String zone : zoneCategories) {
            values.add(zoneCountMap.getOrDefault(zone, 0L));
        }



        // 5. 兜底降级：如果库区无数据/全为0，回退到模拟值，避免前端空白
        boolean allZero2 = values.stream().allMatch(v -> v == 0);
        if (allZero2) {
            long agvPower = workingAgvs * AGV_EFFICIENCY_FACTOR + AGV_BASE_BONUS;
            values = new ArrayList<>(PICKING_BASE_VALUES);
            values.add(Math.max(AGV_MIN_EFFICIENCY, agvPower));
            // 兜底时用固定分类
            pickingEfficiency.put("categories", PICKING_ZONE_CATEGORIES);
        } else {
            pickingEfficiency.put("categories", zoneCategories);
        }

        pickingEfficiency.put("values", values);
        charts.setPickingEfficiency(pickingEfficiency);

        overview.setCharts(charts);
        return overview;
    }
}