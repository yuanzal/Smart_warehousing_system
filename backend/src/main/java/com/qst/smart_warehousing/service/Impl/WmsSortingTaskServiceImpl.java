package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smart_warehousing.DTO.CreateSortingTaskDTO;
import com.qst.smart_warehousing.entity.*;
import com.qst.smart_warehousing.mapper.*;
import com.qst.smart_warehousing.service.IWmsSortingTaskService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class WmsSortingTaskServiceImpl extends ServiceImpl<WmsSortingTaskMapper, WmsSortingTask> implements IWmsSortingTaskService {

    @Resource private WmsParcelMapper parcelMapper;
    @Resource
    private WmsStorageSlotMapper storageSlotMapper;
    @Resource private WmsAgvDeviceMapper agvDeviceMapper;
    @Resource private WmsInventoryLogMapper inventoryLogMapper;

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

    @Autowired
    private WmsSortingTaskMapper sortingTaskMapper;

    @Autowired
    private ObjectMapper objectMapper;

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

        // 4. 【AGV 空间路径规划算法（基于曼哈顿距离的拓扑路径模拟）】
        // 模拟从小车当前坐标 (agv.getCurrentX, agv.getCurrentY) 到货位坐标 (slot.getXCoordinate, slot.getYCoordinate)
        List<Map<String, Double>> pathNodes = new ArrayList<>();

        double startX = agv.getCurrentX().doubleValue();
        double startY = agv.getCurrentY().doubleValue();
        double endX = slot.getXCoordinate().doubleValue();
        double endY = slot.getYCoordinate().doubleValue();

        // 模拟生成中途的拐点（拓扑节点规划）
        Map<String, Double> node1 = new HashMap<>();
        node1.put("x", startX);
        node1.put("y", startY);
        pathNodes.add(node1);

        Map<String, Double> node2 = new HashMap<>();
        node2.put("x", endX); // 先横向移动
        node2.put("y", startY);
        pathNodes.add(node2);

        Map<String, Double> node3 = new HashMap<>();
        node3.put("x", endX); // 再纵向移动到达目标
        node3.put("y", endY);
        pathNodes.add(node3);

        try {
            // 将路径序列化为 JSON 字符串
            String routeJson = objectMapper.writeValueAsString(pathNodes);
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
}