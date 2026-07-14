package com.qst.smart_warehousing.controller;

import com.qst.smart_warehousing.entity.WmsAlertLog;
import com.qst.smart_warehousing.service.IWmsAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "WMS智能运营控制塔-告警看板")
@RestController
@RequestMapping("/wms/operation-alert")
public class WmsAlertController {

    @Resource
    private IWmsAlertService alertService;

    @Operation(summary = "巡检触发：手动/定时巡检库区空间是否爆仓")
    @PostMapping("/check-overflow")
    public ResponseEntity<String> triggerInventoryCheck() {
        boolean isOverflow = alertService.checkInventoryOverflow();
        if (isOverflow) {
            return ResponseEntity.ok("警告：全库区已触及爆仓阈值红线，告警日志已生成并同步孪生大屏！");
        }
        return ResponseEntity.ok("库区容积率处于安全水平，扫描完毕。");
    }

    @Operation(summary = "驾驶舱联动：获取当前全库挂起未处理的严重告警列表")
    @GetMapping("/active-list")
    public ResponseEntity<List<WmsAlertLog>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @Operation(summary = "人工介入：中台操作员消警与故障解除同步")
    @PostMapping("/resolve/{alertId}")
    public ResponseEntity<String> resolveAlert(
            @PathVariable("alertId") Long alertId,
            @RequestParam("operatorId") Long operatorId) {
        boolean success = alertService.resolveAlert(alertId, operatorId);
        if (success) {
            return ResponseEntity.ok("告警数据链解除圆满成功，中台恢复正常排程调度状态！");
        }
        return ResponseEntity.status(500).body("消警失败，未找到该告警流水单号。");
    }
}