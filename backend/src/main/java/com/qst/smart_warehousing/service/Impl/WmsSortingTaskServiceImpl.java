package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smart_warehousing.DTO.CreateSortingTaskDTO;
import com.qst.smart_warehousing.entity.*;
import com.qst.smart_warehousing.mapper.*;
import com.qst.smart_warehousing.service.IWmsAlertService;
import com.qst.smart_warehousing.service.IWmsSortingTaskService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class WmsSortingTaskServiceImpl extends ServiceImpl<WmsSortingTaskMapper, WmsSortingTask> implements IWmsSortingTaskService {

    @Resource private WmsParcelMapper parcelMapper;
    @Resource
    private WmsStorageSlotMapper storageSlotMapper;
    @Resource private WmsAgvDeviceMapper agvDeviceMapper;
    @Resource private WmsInventoryLogMapper inventoryLogMapper;

    @Resource
    private IWmsAlertService alertService;

    @Autowired
    private WmsSortingTaskMapper sortingTaskMapper;

    /**
     * 核心 Story 1.1 & 1.2：创建分拣任务并智能指派 AGV 和目标货位
     */
    @Transactional(rollbackFor = Exception.class)
    public WmsSortingTask createAndAssignTask(CreateSortingTaskDTO dto) {
        // 1. 校验包裹状态 (必须是安全进线或AI核验完毕)
        WmsParcel parcel = parcelMapper.selectById(dto.getParcelId());
        if (parcel == null) {
            throw new RuntimeException("系统异常：未找到对应的包裹档案！");
        }

        // 2. 中台算法模拟：动态寻找当前项目/租户下最适宜的 [待命空闲] AGV 小车
        // 对应 SQL 约束：status = 1 (待命空闲)
        WmsAgvDevice idleAgv = agvDeviceMapper.selectFirstIdle(dto.getProjectId(), dto.getTenantId());
        if (idleAgv == null) {
            throw new RuntimeException("调度中台提示：当前库区无可用空闲AGV，任务进入排队队列");
        }

        // 3. 中台算法模拟：动态寻找当前库区 [空闲] 的推荐目标货位
        // 对应 SQL 约束：status = 0 (空闲)
        WmsStorageSlot targetSlot = storageSlotMapper.selectFirstEmptySlot(dto.getProjectId(), dto.getTenantId());
        if (targetSlot == null) {
            throw new RuntimeException("调度中台提示：全库已爆仓，无可用空闲货位分配！");
        }

        // 4. 构建并落库分拣任务单 (wms_sorting_task)
        WmsSortingTask task = new WmsSortingTask();
        task.setTaskCode("TASK" + System.currentTimeMillis()); // 生产单号生成
        task.setParcelId(parcel.getParcelId());  // 🔥 绑定 parcel_id
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
        idleAgv.setLoadWeight(parcel.getWeight()); // 传感器动态抗载配重
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
        parcel.setCurrentSlotId(task.getTargetSlotId()); // 物理捆绑
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

        // 5. 🔥 核心一步：安全刷入只读流水线历史 (wms_inventory_log)，完美对齐数据库字段
        WmsInventoryLog log = new WmsInventoryLog();
        log.setParcelId(parcel.getParcelId());       // 强校验字段 1
        log.setBarcode(parcel.getBarcode());
        log.setActionType(1);                       // 1 - 自动入库
        log.setFromSlotId(null);                     // 入库时源货位为 NULL
        log.setToSlotId(slot.getSlotId());           // 目标落位货位
        log.setOperatorId(operatorId);               // 操作人员ID
        log.setCreateTime(LocalDateTime.now());
        log.setProjectId(task.getProjectId());       // 强校验字段 2
        log.setTenantId(task.getTenantId());         // 强校验字段 3
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
        // 如果小车电量低于20%，虽然是空闲，但需要拦截并触发报警（为Story 3埋下伏笔）
        if (agv.getBatteryLevel() < 20) {
            agv.setStatus(3); // 变更为自主充电状态
            agvDeviceMapper.updateById(agv);
            return false;
        }

        // 4. 基于 A* 算法与动态路网的物理路径规划
        // ====================================================================

        // 4.1 初始化一个 20x20 的虚拟仓储数字网格矩阵（工业上会从地图配置文件加载）
        // 0 代表通道可通行，1 代表障碍物（固定货架区、设备区）
        int[][] warehouseMap = new int[20][20];

        // 预设固定货架障碍物位置（模拟第 4 行到第 15 行的某些格子是货架，小车不能穿过去）
        for (int i = 4; i <= 15; i++) {
            warehouseMap[i][5] = 1;
            warehouseMap[i][10] = 1;
        }

        // 4.2 💡 动态负载均衡与安全联动：从数据库读取当前正处于故障维修(status=3)的货位
        // 动态将其加入地图障碍物，防止 AGV 走这条通道或者撞向维修区
        List<WmsStorageSlot> defectSlots = storageSlotMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WmsStorageSlot>()
                        .eq("status", 3)
        );
        for (WmsStorageSlot defect : defectSlots) {
            // 将浮点坐标强转/映射为网格索引
            int obsX = (int) Math.round(defect.getXCoordinate().doubleValue());
            int obsY = (int) Math.round(defect.getYCoordinate().doubleValue());
            if (obsX >= 0 && obsX < 20 && obsY >= 0 && obsY < 20) {
                warehouseMap[obsX][obsY] = 1; // 标记为动态碰撞体积
            }
        }

        // 4.3 坐标对齐转换：将 AGV 当前的实数米制坐标，映射为网格的整数坐标点
        int startGridX = (int) Math.round(agv.getCurrentX().doubleValue());
        int startGridY = (int) Math.round(agv.getCurrentY().doubleValue());
        int endGridX = (int) Math.round(slot.getXCoordinate().doubleValue());
        int endGridY = (int) Math.round(slot.getYCoordinate().doubleValue());

        // 4.4 调用 A* 寻路器算法计算最优拓扑节点
        List<int[]> calculatedGridPath = AStarPathFinder.findPath(
                warehouseMap, startGridX, startGridY, endGridX, endGridY
        );

        if (calculatedGridPath.isEmpty()) {
            // 算法判定没有安全通路可达，直接触发熔断告警
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
            // 将真实计算出来的多维拐点序列化
            String routeJson = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.getObjectMapper()
                    .writeValueAsString(realPathNodes);

            agv.setRoutePathJson(routeJson);

            // 路径计算完毕后，更新小车路径，同时将任务状态推至状态机阶段 2（路由计算完毕）
            agvDeviceMapper.updateById(agv);

            task.setStatus(2); // 路由计算完毕
            sortingTaskMapper.updateById(task);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateAgvLocation(Long agvId, Double currentX, Double currentY, Integer batteryLevel) {
        WmsAgvDevice agv = agvDeviceMapper.selectById(agvId);
        if (agv == null) {
            return false;
        }

        // 动态更新小车的物理坐标和电量
        agv.setCurrentX(java.math.BigDecimal.valueOf(currentX));
        agv.setCurrentY(java.math.BigDecimal.valueOf(currentY));
        agv.setBatteryLevel(batteryLevel);

        // TODO：【工业拓展】如果在实际高吞吐业务中，此处还会通过 WebSocket 或 Redis Pub/Sub 将坐标秒级推给前端 3D 数字孪生大屏
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
}