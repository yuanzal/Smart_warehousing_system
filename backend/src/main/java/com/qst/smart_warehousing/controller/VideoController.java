package com.qst.smart_warehousing.controller;

import com.qst.smart_warehousing.DTO.VideoStreamDTO;
import com.qst.smart_warehousing.entity.Result;          // 使用现有的 Result 类
import com.qst.smart_warehousing.service.VideoPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video")
@Tag(name = "视频流管理")
public class VideoController {

    @Autowired
    private VideoPushService videoPushService;

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello world";
    }

    @PostMapping("/start2")
    @Operation(summary = "启动视频推流")
    public Result<VideoStreamDTO> startPush2(
            @RequestParam(name = "streamId") String streamId,
            @RequestParam(name = "rtspUrl", required = false) String rtspUrl) {
        System.out.println("=== VideoController.startPush2 POST 被调用 ===");
        try {
            VideoStreamDTO dto = videoPushService.startPush(streamId, rtspUrl);
            return Result.ok(dto);   // 使用现有的 Result.ok(data)
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "启动推流失败: " + e.getMessage());
        }
    }

    @PostMapping("/stop")
    @Operation(summary = "停止视频推流")
    public Result<String> stopPush(@RequestParam String streamId) {
        System.out.println("=== VideoController.stopPush 被调用 ===");
        try {
            boolean success = videoPushService.stopPush(streamId);
            if (success) {
                return Result.ok("推流已停止");
            } else {
                return Result.error(404, "流不存在或已停止");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "停止推流失败: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    @Operation(summary = "查询推流状态")
    public Result<VideoStreamDTO> getStatus(@RequestParam String streamId) {
        System.out.println("=== VideoController.getStatus 被调用 ===");
        VideoStreamDTO dto = videoPushService.getStatus(streamId);
        return Result.ok(dto);
    }
}