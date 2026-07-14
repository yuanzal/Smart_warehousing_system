package com.qst.smart_warehousing.service;

import com.qst.smart_warehousing.entity.WmsAlertLog;
import java.util.List;

public interface IWmsAlertService {
    // 检查库区是否触及爆仓红线
    boolean checkInventoryOverflow();

    // 触发分拣物理阻挡与卡死告警通知
    void triggerSortingExceptionAlert(Long taskId, Long agvId, String hardwareError);

    // 查询当前未处理的挂起告警列表（供给前端大屏轮询或弹窗）
    List<WmsAlertLog> getActiveAlerts();

    // 人工/中台确认处理告警
    boolean resolveAlert(Long alertId, Long operatorId);
}