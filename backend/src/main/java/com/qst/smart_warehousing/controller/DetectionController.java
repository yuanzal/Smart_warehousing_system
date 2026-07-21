package com.qst.smart_warehousing.controller;

import com.qst.smart_warehousing.client.AiInferenceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class DetectionController {

    @Autowired
    private AiInferenceClient aiClient;

    @PostMapping("/detect")
    public Mono<Map<String, Object>> detect(
            @RequestParam String videoSource,
            @RequestParam(defaultValue = "1.0") float intervalSec,
            @RequestParam(defaultValue = "0.5") float confThreshold,
            @RequestParam(defaultValue = "10") int maxFrames) {

        return aiClient.detect(videoSource, intervalSec, confThreshold, maxFrames)
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("code", 200);
                    response.put("data", result);
                    return response;
                })
                .onErrorResume(e -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("code", 500);
                    errorResponse.put("msg", "AI 服务调用失败: " + e.getMessage());
                    return Mono.just(errorResponse);
                });
    }
}