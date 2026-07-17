package com.qst.smart_warehousing.controller;

import com.qst.smart_warehousing.DTO.DeviceDataDTO;
import com.qst.smart_warehousing.entity.Result;
import com.qst.smart_warehousing.service.DeviceCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/device")
@Tag(name = "边缘设备数据接入")
public class DeviceDataController {

    @Autowired
    private DeviceCacheService deviceCacheService;

    @PostMapping("/report")
    @Operation(summary = "设备数据上报")
    public Result<String> reportDeviceData(@RequestBody DeviceDataDTO dto) {
        try {
            deviceCacheService.updateDeviceStatus(dto);
            return Result.ok("数据上报成功");
        } catch (Exception e) {
            return Result.error(500, "数据上报失败: " + e.getMessage());
        }
    }

    @GetMapping("/status/all")
    @Operation(summary = "获取所有设备最新状态")
    public Result<Map<Object, Object>> getAllDeviceStatus() {
        return Result.ok(deviceCacheService.getAllDeviceStatus());
    }

    @GetMapping("/status/{deviceId}")
    @Operation(summary = "获取单个设备状态")
    public Result<Object> getDeviceStatus(@PathVariable String deviceId) {
        Object status = deviceCacheService.getDeviceStatus(deviceId);
        if (status == null) {
            return Result.error(404, "设备未找到或已离线");
        }
        return Result.ok(status);
    }

    @GetMapping("/agv/positions")
    @Operation(summary = "获取所有 AGV 实时位置")
    public Result<Map<Object, Object>> getAllAgvPositions() {
        return Result.ok(deviceCacheService.getAllAgvPositions());
    }
}