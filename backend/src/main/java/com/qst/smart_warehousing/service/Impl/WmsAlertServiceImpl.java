package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qst.smart_warehousing.entity.WmsAlertLog;
import com.qst.smart_warehousing.entity.WmsAlertRule;
import com.qst.smart_warehousing.entity.WmsStorageSlot;
import com.qst.smart_warehousing.mapper.WmsAlertLogMapper;
import com.qst.smart_warehousing.mapper.WmsAlertRuleMapper;
import com.qst.smart_warehousing.mapper.WmsStorageSlotMapper;
import com.qst.smart_warehousing.service.IWmsAlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class WmsAlertServiceImpl implements IWmsAlertService {

    @Resource
    private WmsAlertRuleMapper alertRuleMapper;
    @Resource
    private WmsAlertLogMapper alertLogMapper;
    @Resource
    private WmsStorageSlotMapper storageSlotMapper;

    /**
     * 1. 🔍 实时扫描库区物理容量状态（核心算法：利用数据库聚合计算爆仓率）
     */
    @Override
    @Transactional
    public boolean checkInventoryOverflow() {
        // A. 获取激活中的爆仓告警规则
        WmsAlertRule rule = alertRuleMapper.selectOne(
                new QueryWrapper<WmsAlertRule>().eq("rule_type", 1).eq("is_enabled", 1)
        );
        if (rule == null) return false;

        // B. 统计总货位数与被占用货位数 (假设 status != 0 代表非空闲占用)
        Long totalSlots = storageSlotMapper.selectCount(new QueryWrapper<WmsStorageSlot>());
        if (totalSlots == 0) return false;

        Long occupiedSlots = storageSlotMapper.selectCount(
                new QueryWrapper<WmsStorageSlot>().ne("status", 0)
        );

        // C. 计算实时占用率
        BigDecimal usageRate = BigDecimal.valueOf(occupiedSlots)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalSlots), 2, RoundingMode.HALF_UP);

        System.out.println(rule.getThreshold());
        // D. 判定是否跃过规则设定的爆仓红线
        if (usageRate.compareTo(rule.getThreshold()) >= 0) {
            // 触发爆仓日志持久化
            WmsAlertLog alertLog = new WmsAlertLog();
            alertLog.setRuleId(rule.getRuleId());
            alertLog.setAlertType(1);
            alertLog.setAlertLevel(rule.getAlertLevel());
            alertLog.setStatus(0); // 未处理
            alertLog.setAlertContent(String.format("【WMS爆仓熔断警告】当前库区总货位 %d 个，已占用 %d 个，实时空间占用率达到 %s%%，已超过系统安全红线 %s%%！请立刻停止进线排程！",
                    totalSlots, occupiedSlots, usageRate, rule.getThreshold()));

            alertLogMapper.insert(alertLog);

            // 【大屏互锁】在实际生产环境中，这里会触发 WebSocket 或者是我们在 Story 3 里设计的 MQ 广播，通知 3D 数字孪生大屏全屏飘红闪烁
            System.err.println(alertLog.getAlertContent());
            return true;
        }
        return false;
    }

    /**
     * 2. 🚨 分拣物理设备异常实时告警（由 Story 3 的小车故障回调联动触发）
     */
    @Override
    @Transactional
    public void triggerSortingExceptionAlert(Long taskId, Long agvId, String hardwareError) {
        WmsAlertRule rule = alertRuleMapper.selectOne(
                new QueryWrapper<WmsAlertRule>().eq("rule_type", 2).eq("is_enabled", 1)
        );

        WmsAlertLog alertLog = new WmsAlertLog();
        alertLog.setRuleId(rule != null ? rule.getRuleId() : null);
        alertLog.setAlertType(2); // 设备异常
        alertLog.setAlertLevel("CRITICAL"); // 最高级异常
        alertLog.setStatus(0);
        alertLog.setAlertContent(String.format("【AGV物理碰撞阻挡熔断】任务单号: %d, 执行小车: %d 号AGV 发生物理故障死锁！底层硬件反馈: %s。中台已将该车辆和路径节点从全局拓扑树中动态阻断屏蔽！",
                taskId, agvId, hardwareError));

        alertLogMapper.insert(alertLog);
    }

    @Override
    public List<WmsAlertLog> getActiveAlerts() {
        return alertLogMapper.selectList(new QueryWrapper<WmsAlertLog>().eq("status", 0));
    }

    @Override
    @Transactional
    public boolean resolveAlert(Long alertId, Long operatorId) {
        WmsAlertLog log = alertLogMapper.selectById(alertId);
        if (log == null) return false;
        log.setStatus(1); // 变更为已确认/已处理
        log.setHandlerId(operatorId);
        return alertLogMapper.updateById(log) > 0;
    }
}