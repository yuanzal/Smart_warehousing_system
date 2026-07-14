package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class WmsDashboardServiceImpl implements IWmsDashboardService {

    private static final String DASHBOARD_CACHE_KEY = "wms:dashboard:overview";
    private static final long CACHE_TTL_SECONDS = 10; // 💡 10秒短缓存，既能保证大屏“基本实时”，又能阻断 99% 的直接探库请求

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper; // Spring Boot 默认注入的 Jackson 序列化器

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
        // 🌟 核心设计：第一层缓存拦截
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(DASHBOARD_CACHE_KEY);
            if (cachedJson != null) {
                // 命中缓存，直接反序列化返回，耗时 < 5ms
                return objectMapper.readValue(cachedJson, WmsDashboardOverviewVO.class);
            }
        } catch (Exception e) {
            // 💡 高可用降级：如果 Redis 连接断开或异常，记录日志，继续走数据库，不让接口报错
            log.error("【高可用降级】Redis缓存读取异常，自动切回数据库查询：{}", e.getMessage());
        }

        // 🌟 核心设计：第二层双重锁，防止高并发下缓存失效瞬间万级并发直接打垮 MySQL（防缓存击穿）
        synchronized (this) {
            try {
                // 再次尝试从缓存拿，可能刚才被前一个抢到锁的线程写入了
                String cachedJson = stringRedisTemplate.opsForValue().get(DASHBOARD_CACHE_KEY);
                if (cachedJson != null) {
                    return objectMapper.readValue(cachedJson, WmsDashboardOverviewVO.class);
                }
            } catch (Exception ignored) {
            }

            log.info("【🔥 缓存未命中/失效】开始执行 MySQL 深度聚合计算...");
            // 执行真正的繁重 SQL 聚合（原来的那套逻辑）
            WmsDashboardOverviewVO overview = getOverviewFromDatabase();

            // 🌟 核心设计：异步写入缓存，设置 TTL
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
     * 提取出的纯数据库查询聚合方法
     */
    private WmsDashboardOverviewVO getOverviewFromDatabase() {
        WmsDashboardOverviewVO overview = new WmsDashboardOverviewVO();
        overview.setTimestamp(System.currentTimeMillis());

        // A. KPI 核心指标
        WmsDashboardKpiVO kpi = new WmsDashboardKpiVO();
        Long totalTasks = sortingTaskMapper.selectCount(null);
        Long completedTasks = sortingTaskMapper.selectCount(new QueryWrapper<WmsSortingTask>().eq("status", 4));
        kpi.setTotalTasks(totalTasks);
        kpi.setCompletedTasks(completedTasks);

        if (totalTasks > 0) {
            kpi.setCompletionRate(BigDecimal.valueOf(completedTasks).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalTasks), 2, RoundingMode.HALF_UP));
        } else {
            kpi.setCompletionRate(BigDecimal.ZERO);
        }

        kpi.setActiveAlerts(alertLogMapper.selectCount(new QueryWrapper<WmsAlertLog>().eq("status", 0)));

        Long totalAgvs = agvDeviceMapper.selectCount(null);
        Long workingAgvs = agvDeviceMapper.selectCount(new QueryWrapper<WmsAgvDevice>().eq("status", 2));
        kpi.setAgvUtilizationRate(totalAgvs > 0 ? BigDecimal.valueOf(workingAgvs).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalAgvs), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        Long totalSlots = storageSlotMapper.selectCount(null);
        Long occupiedSlots = storageSlotMapper.selectCount(new QueryWrapper<WmsStorageSlot>().ne("status", 0));
        kpi.setStorageOccupancyRate(totalSlots > 0 ? BigDecimal.valueOf(occupiedSlots).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalSlots), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        overview.setKpi(kpi);

        // B. 图表 Group By
        WmsDashboardChartsVO charts = new WmsDashboardChartsVO();
        QueryWrapper<WmsSortingTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.select("status as name", "count(*) as value").groupBy("status");
        charts.setTaskStatusDistribution(sortingTaskMapper.selectMaps(taskWrapper));

        QueryWrapper<WmsAlertLog> alertWrapper = new QueryWrapper<>();
        alertWrapper.select("alert_level as name", "count(*) as value").groupBy("alert_level");
        charts.setAlertLevelDistribution(alertLogMapper.selectMaps(alertWrapper));
        overview.setCharts(charts);

        return overview;
    }
}