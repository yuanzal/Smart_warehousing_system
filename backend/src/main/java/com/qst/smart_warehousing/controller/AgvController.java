package com.qst.smart_warehousing.controller;

import com.qst.smart_warehousing.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO:这是模拟路线，实际回调接口已经存在于WmsSortingTaskController，后面使用设备回调展示路线
@RestController
@RequestMapping("/api/agv")
public class AgvController {

    @GetMapping("/positions")
    public Result<List<Map<String, Object>>> getAgvPositions() {
        long time = System.currentTimeMillis() / 1000;
        List<Map<String, Object>> agvList = new ArrayList<>();
        // 生成 4 辆 AGV 的模拟位置（沿椭圆轨迹移动）
        for (int i = 0; i < 4; i++) {
            double phase = i * Math.PI / 2;
            double radius = 5 + i * 0.5;
            double x = Math.sin(time * 0.3 + phase) * radius;
            double z = Math.cos(time * 0.3 + phase) * (radius * 0.7);
            double angle = -Math.atan2(Math.cos(time * 0.3 + phase) * (radius * 0.7),
                    Math.sin(time * 0.3 + phase) * radius);
            agvList.add(Map.of(
                    "id", i + 1,
                    "x", x,
                    "z", z,
                    "angle", angle
            ));
        }
        return Result.ok(agvList);
    }
}