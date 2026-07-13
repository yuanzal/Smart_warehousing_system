package com.qst.smart_warehousing.controller;

import com.qst.smart_warehousing.DTO.CreateSortingTaskDTO;
import com.qst.smart_warehousing.entity.WmsSortingTask;
import com.qst.smart_warehousing.service.Impl.WmsSortingTaskServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "智能分拣调度控制台")
@RestController
@RequestMapping("/wms/sorting-task")
public class WmsSortingTaskController {

    @Resource
    private WmsSortingTaskServiceImpl sortingTaskService;

    @Operation(summary = "进线触发：创建分拣任务并分拨硬件")
    @PostMapping("/create-assign")
    public ResponseEntity<WmsSortingTask> createAndAssign(@RequestBody CreateSortingTaskDTO dto) {
        return ResponseEntity.ok(sortingTaskService.createAndAssignTask(dto));
    }

    @Operation(summary = "物理落位：全工况自动/人工完工核验")
    @PostMapping("/complete/{taskId}")
    public ResponseEntity<?> completeTask(
            @PathVariable("taskId") Long taskId,
            @RequestParam("operatorId") Long operatorId){
        sortingTaskService.completeSortingTask(taskId, operatorId);
        return ResponseEntity.ok("落位数据链同步圆满成功，库存流水已生成！");
    }


    /**
     * 🚀 Story 2 核心接口：对转运中的任务进行路径规划与动态动态调度优化
     */
    @PostMapping("/optimize-route/{taskId}")
    public ResponseEntity<?> optimizeRouteAndLoadBalance(
            @PathVariable("taskId") Long taskId,
            @RequestParam("projectId") Integer projectId,
            @RequestParam("tenantId") Integer tenantId) {

        boolean success = sortingTaskService.calculateRouteAndBalance(taskId, projectId, tenantId);

        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("code", "200");
            response.put("message", "AGV路径规划与工位负载均衡计算成功，调度指令已下发拓扑网络！");
            return ResponseEntity.ok(response);
        } else {
            response.put("code", "500");
            response.put("message", "调度算法优化失败，请检查仓储物理设备状态。");
            return ResponseEntity.status(500).body(response);
        }
    }
}