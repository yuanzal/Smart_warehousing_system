package com.qst.smart_warehousing.controller;

import com.qst.smart_warehousing.entity.Result;
import com.qst.smart_warehousing.entity.WmsAlertLog;
import com.qst.smart_warehousing.service.IWmsAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public Result<Boolean> triggerInventoryCheck() {
        boolean isOverflow = alertService.checkInventoryOverflow();
        if (isOverflow) {
            return Result.ok(true);
        }
        return Result.ok(false);
    }

    @Operation(summary = "驾驶舱联动：获取当前全库挂起未处理的严重告警列表")
    @GetMapping("/active-list")
    public Result<List<WmsAlertLog>> getActiveAlerts() {
        List<WmsAlertLog> alertList = alertService.getActiveAlerts();
        return Result.ok(alertList);
    }

    @Operation(summary = "人工介入：中台操作员消警与故障解除同步")
    @PostMapping("/resolve/{alertId}")
    public Result<String> resolveAlert(
            @PathVariable("alertId") Long alertId,
            @RequestParam("operatorId") Long operatorId) {
        boolean success = alertService.resolveAlert(alertId, operatorId);
        if (success) {
            return Result.ok("告警解除成功");
        }
        return Result.error(500,"消警失败，未找到该告警流水单号");
    }
}