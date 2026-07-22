package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qst.smart_warehousing.DTO.CreateSortingTaskDTO;
import com.qst.smart_warehousing.DTO.SortingTaskQueryDTO;
import com.qst.smart_warehousing.DTO.WmsAgvRealtimeLocDTO;
import com.qst.smart_warehousing.VO.SortingTaskVO;
import com.qst.smart_warehousing.VO.TaskOverviewVO;
import com.qst.smart_warehousing.entity.*;
import com.qst.smart_warehousing.mapper.*;
import com.qst.smart_warehousing.service.IWmsAlertService;
import com.qst.smart_warehousing.service.IWmsSortingTaskService;
import com.qst.smart_warehousing.util.SecurityUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WmsSortingTaskServiceImpl extends ServiceImpl<WmsSortingTaskMapper, WmsSortingTask> implements IWmsSortingTaskService {

    @Resource private WmsParcelMapper parcelMapper;
    @Resource private WmsStorageSlotMapper storageSlotMapper;
    @Resource private WmsAgvDeviceMapper agvDeviceMapper;
    @Resource private WmsInventoryLogMapper inventoryLogMapper;
    @Resource private IWmsAlertService alertService;
    @Autowired private WmsSortingTaskMapper sortingTaskMapper;

    // 💡 导入 Redis 常量
    private static final String AGV_REALTIME_KEY = "wms:agv:realtime";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    // 工业级设计：创建专用于异步持久化 MySQL 的隔离线程池，防止 IoT 高频写 I/O 阻塞系统公共线程
    private final ExecutorService dbWriteExecutor = new ThreadPoolExecutor(
            4, // 核心线程数
            8, // 最大线程数
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000), // 缓冲队列，可承受突发的高频心跳堆积
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "agv-async-db-writer-" + counter.getAndIncrement());
                    thread.setDaemon(true); // 守护线程，随主 JVM 退出
                    return thread;
                }
            },
            new ThreadPoolExecutor.DiscardOldestPolicy() // 防御策略：万一队列满了，丢弃最老的位置上报任务，确保系统不 OOM
    );

    /**
     * 核心 Story 1.1 & 1.2：创建分拣任务并智能指派 AGV 和目标货位
     */
    @Transactional(rollbackFor = Exception.class)
    public WmsSortingTask createAndAssignTask(CreateSortingTaskDTO dto) {
        // 1. 校验包裹状态
        WmsParcel parcel = parcelMapper.selectById(dto.getParcelId());
        if (parcel == null) {
            throw new RuntimeException("系统异常：未找到对应的包裹档案！");
        }

        // 2. 中台算法模拟：动态寻找当前项目/租户下最适宜的 [待命空闲] AGV 小车
        WmsAgvDevice idleAgv = agvDeviceMapper.selectFirstIdle(dto.getProjectId(), dto.getTenantId());
        if (idleAgv == null) {
            throw new RuntimeException("调度中台提示：当前库区无可用空闲AGV，任务进入排队队列");
        }

        // 3. 中台算法模拟：动态寻找当前库区 [空闲] 的推荐目标货位
        WmsStorageSlot targetSlot = storageSlotMapper.selectFirstEmptySlot(dto.getProjectId(), dto.getTenantId());
        if (targetSlot == null) {
            throw new RuntimeException("调度中台提示：全库已爆仓，无可用空闲货位分配！");
        }

        // 4. 构建并落库分拣任务单
        WmsSortingTask task = new WmsSortingTask();
        task.setTaskCode("TASK" + System.currentTimeMillis());
        task.setParcelId(parcel.getParcelId());
        task.setBarcode(dto.getBarcode());
        task.setSourceStationId(dto.getSourceStationId());
        task.setTargetSlotId(targetSlot.getSlotId());
        task.setAssignedAgvId(idleAgv.getAgvId());
        task.setStatus(3); // 直接由 创建(1) 跳跃至 AGV转运中(3)
        task.setCreateTime(LocalDateTime.now());
        task.setStartTime(LocalDateTime.now());
        task.setProjectId(dto.getProjectId());
        task.setTenantId(dto.getTenantId());
        this.baseMapper.insert(task);

        // 5. 联动更新包裹状态 -> 分拣流转中(3)
        parcel.setStatus(3);
        parcelMapper.updateById(parcel);

        // 6. 联动锁定物理货位状态 -> 锁定(2)
        targetSlot.setStatus(2);
        storageSlotMapper.updateById(targetSlot);

        // 7. 联动变更 AGV 小车状态 -> 配送执行中(2)，并模拟测重赋权
        idleAgv.setStatus(2);
        idleAgv.setLoadWeight(parcel.getWeight());
        agvDeviceMapper.updateById(idleAgv);

        return task;
    }

    /**
     * 核心 Story 1.3：分拣落位圆满完工，触发入库历史流水记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeSortingTask(Long taskId, Long operatorId) {
        WmsSortingTask task = this.baseMapper.selectById(taskId);
        if (task == null || task.getStatus() == 4) {
            throw new RuntimeException("任务不存在或该任务已完成上架归档");
        }

        // 1. 完工归档任务单状态 -> 机械臂已成功分拣入库(4)
        task.setStatus(4);
        task.setEndTime(LocalDateTime.now());
        this.baseMapper.updateById(task);

        // 2. 更新包裹状态 -> 已上架入库(4)，并刷入当前所在货位ID
        WmsParcel parcel = parcelMapper.selectById(task.getParcelId());
        parcel.setStatus(4);
        parcel.setCurrentSlotId(task.getTargetSlotId());
        parcelMapper.updateById(parcel);

        // 3. 更新货位状态 -> 已占用(1)
        WmsStorageSlot slot = storageSlotMapper.selectById(task.getTargetSlotId());
        slot.setStatus(1);
        storageSlotMapper.updateById(slot);

        // 4. 释放 AGV 小车 -> 回归待命空闲(1)，测重载荷归零
        WmsAgvDevice agv = agvDeviceMapper.selectById(task.getAssignedAgvId());
        agv.setStatus(1);
        agv.setLoadWeight(BigDecimal.ZERO);
        agvDeviceMapper.updateById(agv);

        // 5. 安全刷入只读流水线历史 (wms_inventory_log)，完美对齐数据库字段
        WmsInventoryLog log = new WmsInventoryLog();
        log.setParcelId(parcel.getParcelId());
        log.setBarcode(parcel.getBarcode());
        log.setActionType(1);                       // 1 - 自动入库
        log.setFromSlotId(null);
        log.setToSlotId(slot.getSlotId());
        log.setOperatorId(operatorId);
        log.setCreateTime(LocalDateTime.now());
        log.setProjectId(task.getProjectId());
        log.setTenantId(task.getTenantId());
        inventoryLogMapper.insert(log);
    }

    @Override
    @Transactional
    public boolean calculateRouteAndBalance(Long taskId, Integer projectId, Integer tenantId) {
        // 1. 获取当前任务单
        WmsSortingTask task = sortingTaskMapper.selectById(taskId);
        if (task == null || task.getAssignedAgvId() == null || task.getTargetSlotId() == null) {
            return false;
        }

        // 2. 获取分配的小车与目标货位
        WmsAgvDevice agv = agvDeviceMapper.selectById(task.getAssignedAgvId());
        WmsStorageSlot slot = storageSlotMapper.selectById(task.getTargetSlotId());
        if (agv == null || slot == null) {
            return false;
        }

        // 3. 【工位负载均衡与安全策略策略】
        if (agv.getBatteryLevel() < 20) {
            agv.setStatus(3); // 变更为自主充电状态
            agvDeviceMapper.updateById(agv);
            return false;
        }

        // 4. 基于 A* 算法与动态路网的物理路径规划
        int[][] warehouseMap = new int[20][20];

        // 预设固定货架障碍物位置
        for (int i = 4; i <= 15; i++) {
            warehouseMap[i][5] = 1;
            warehouseMap[i][10] = 1;
        }

        // 4.2 动态负载均衡与安全联动：从数据库读取当前正处于故障维修(status=3)的货位
        List<WmsStorageSlot> defectSlots = storageSlotMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WmsStorageSlot>()
                        .eq("status", 3)
        );
        for (WmsStorageSlot defect : defectSlots) {
            int obsX = (int) Math.round(defect.getXCoordinate().doubleValue());
            int obsY = (int) Math.round(defect.getYCoordinate().doubleValue());
            if (obsX >= 0 && obsX < 20 && obsY >= 0 && obsY < 20) {
                warehouseMap[obsX][obsY] = 1;
            }
        }

        // 4.3 坐标对齐转换
        int startGridX = (int) Math.round(agv.getCurrentX().doubleValue());
        int startGridY = (int) Math.round(agv.getCurrentY().doubleValue());
        int endGridX = (int) Math.round(slot.getXCoordinate().doubleValue());
        int endGridY = (int) Math.round(slot.getYCoordinate().doubleValue());

        // 4.4 调用 A* 寻路器算法计算最优拓扑节点
        List<int[]> calculatedGridPath = AStarPathFinder.findPath(
                warehouseMap, startGridX, startGridY, endGridX, endGridY
        );

        if (calculatedGridPath.isEmpty()) {
            return false;
        }

        // 4.5 将算法计算出的网格坐标转换为大屏能识别的 3D 物理空间坐标 JSON 串
        List<Map<String, Double>> realPathNodes = new ArrayList<>();
        for (int[] gridPoint : calculatedGridPath) {
            Map<String, Double> point = new HashMap<>();
            point.put("x", (double) gridPoint[0]);
            point.put("y", (double) gridPoint[1]);
            realPathNodes.add(point);
        }

        try {
            String routeJson = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.getObjectMapper()
                    .writeValueAsString(realPathNodes);

            agv.setRoutePathJson(routeJson);
            agvDeviceMapper.updateById(agv);

            task.setStatus(2); // 路由计算完毕
            sortingTaskMapper.updateById(task);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 🌟 重构后的核心方法：AGV 实时位置高频写入 Redis 缓存，并通过独立线程异步回写 MySQL
     */
    @Override
    public boolean updateAgvLocation(Long agvId, Double currentX, Double currentY, Integer batteryLevel) {
        // 1. 组装强类型实时坐标 DTO
        WmsAgvRealtimeLocDTO loc = new WmsAgvRealtimeLocDTO(
                agvId, currentX, currentY, batteryLevel, System.currentTimeMillis()
        );

        boolean redisSuccess = false;

        try {
            // 2. ⚡ 高性能内存化拦截：秒级写入 Redis Hash (时间复杂度 O(1)，可承受万级 TPS)
            String jsonStr = objectMapper.writeValueAsString(loc);
            stringRedisTemplate.opsForHash().put(AGV_REALTIME_KEY, agvId.toString(), jsonStr);
            redisSuccess = true;
        } catch (Exception e) {
            log.error("【Redis缓存写入失败】AGV {} 位置未能写入 Redis，准备执行降级兜底：{}", agvId, e.getMessage());
        }

        // 3. 异步/降级逻辑处理 (Write-Behind)
        if (redisSuccess) {
            // 🟢 Redis 写入成功：通过独立线程池【异步】回写 MySQL，彻底消除磁盘 IO 瓶颈，主线程毫秒级秒回！
            CompletableFuture.runAsync(() -> {
                try {
                    syncToDatabase(agvId, currentX, currentY, batteryLevel);
                } catch (Exception e) {
                    log.error("【MySQL异步回写异常】AGV {} 数据落库失败: {}", agvId, e.getMessage());
                }
            }, dbWriteExecutor);
            return true;
        } else {
            // 容灾退化防线：如果 Redis 挂了，强制降级为【同步】直写 MySQL，确保高可用与数据零丢失！
            log.warn("【高可用降级】Redis 异常，AGV {} 的位置上报转换为同步写 MySQL 兜底", agvId);
            return syncToDatabase(agvId, currentX, currentY, batteryLevel);
        }
    }

    /**
     * 辅助持久化逻辑：抽取原有的物理落库操作（带事务隔离）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean syncToDatabase(Long agvId, Double currentX, Double currentY, Integer batteryLevel) {
        WmsAgvDevice agv = agvDeviceMapper.selectById(agvId);
        if (agv == null) {
            log.warn("【回写警告】数据库中未找到 ID 为 {} 的 AGV 设备档案", agvId);
            return false;
        }
        agv.setCurrentX(BigDecimal.valueOf(currentX));
        agv.setCurrentY(BigDecimal.valueOf(currentY));
        agv.setBatteryLevel(batteryLevel);
        System.out.println(BigDecimal.valueOf(currentX));
        System.out.println(BigDecimal.valueOf(currentY));
        return agvDeviceMapper.updateById(agv) > 0;
    }

    /**
     * 2. 分拣任务设备执行回调与防灾拦截
     */
    @Override
    @Transactional
    public boolean handleTaskCallback(Long taskId, Long agvId, Integer statusCallback, String errorMessage) {
        WmsSortingTask task = sortingTaskMapper.selectById(taskId);
        WmsAgvDevice agv = agvDeviceMapper.selectById(agvId);

        if (task == null || agv == null) {
            return false;
        }

        // 判断回调状态
        if (statusCallback == 4 || statusCallback == 5) {
            // 🚨 触发物理异常拦截：小车在中途发生碰撞或卡死
            agv.setStatus(4); // 小车变更为：故障/维修状态
            agvDeviceMapper.updateById(agv);

            // 任务单挂起，等待人工介入核验
            task.setStatus(5); // 任务状态变更为：物理异常挂起
            sortingTaskMapper.updateById(task);

            // 💡 核心联动：自动往运营告警看板里记一笔严重设备故障！
            alertService.triggerSortingExceptionAlert(taskId, agvId, errorMessage);

            System.err.println("【WMS调度中台熔断警告】AGV " + agvId + " 触发异常回调，原因: " + errorMessage);
            return true;
        } else if (statusCallback == 3) {
            // 🟢 正常转运回调：仅更新状态
            task.setStatus(3); // 转运中
            sortingTaskMapper.updateById(task);
            return true;
        }

        return false;
    }
    @Override
    public IPage<SortingTaskVO> getTaskPage(SortingTaskQueryDTO queryDTO) {
        Integer tenantId = SecurityUtil.getTenantId(); // 替换成你项目的上下文工具
        Page<SortingTaskVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return baseMapper.selectTaskPage(page, queryDTO, tenantId);
    }

    @Override
    public TaskOverviewVO getTodayOverview() {
        Integer tenantId = SecurityUtil.getTenantId();
        LocalDate today = LocalDate.now();

        // 按状态分组统计今日任务
        List<Map<String, Object>> statusCount = this.list(new QueryWrapper<WmsSortingTask>()
                        .select("status, count(*) as cnt")
                        .eq("tenant_id", tenantId)
                        .apply("DATE(create_time) = {0}", today)
                        .groupBy("status")
                ).stream().map(task -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("status", task.getStatus());
                    map.put("cnt", 1);
                    return map;
                }).collect(Collectors.groupingBy(m -> m.get("status")))
                .entrySet().stream().map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("status", e.getKey());
                    map.put("cnt", e.getValue().size());
                    return map;
                }).collect(Collectors.toList());

        TaskOverviewVO vo = new TaskOverviewVO();
        int total = 0;
        for (Map<String, Object> map : statusCount) {
            Integer status = (Integer) map.get("status");
            Integer cnt = ((Number) map.get("cnt")).intValue();
            total += cnt;
            if (status == 3) vo.setTransportingCount(cnt);
            if (status == 4) vo.setCompletedCount(cnt);
            if (status == 5) vo.setExceptionCount(cnt);
        }
        vo.setTotalCount(total);

        // 空值兜底
        if (vo.getTransportingCount() == null) vo.setTransportingCount(0);
        if (vo.getCompletedCount() == null) vo.setCompletedCount(0);
        if (vo.getExceptionCount() == null) vo.setExceptionCount(0);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryTask(Long taskId) {
        WmsSortingTask task = this.getById(taskId);
        if (task == null || task.getStatus() != 5) {
            return false;
        }
        // 重置为路由计算状态，清空AGV分配等待重新调度
        task.setStatus(2);
        task.setAssignedAgvId(null);
        task.setEndTime(null);
        return this.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(Long taskId) {
        WmsSortingTask task = this.getById(taskId);
        if (task == null || !Arrays.asList(1, 2, 3).contains(task.getStatus())) {
            return false;
        }
        // 置为异常终止状态
        task.setStatus(5);
        task.setEndTime(LocalDateTime.now());
        return this.updateById(task);
    }
}