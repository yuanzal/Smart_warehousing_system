package com.qst.smart_warehousing.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AiInferenceClient {

    private final WebClient webClient;

    public AiInferenceClient(WebClient.Builder builder,
                             @Value("${ai.service.url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * 调用 Python AI 服务的同步检测接口
     * @param videoSource 视频源（RTSP/文件路径/摄像头索引）
     * @param intervalSec 抽帧间隔（秒）
     * @param confThreshold 置信度阈值
     * @param maxFrames 最大检测帧数
     * @return 检测结果 Map
     */
    public Mono<Map> detect(String videoSource, float intervalSec, float confThreshold, int maxFrames) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/detect/start")
                        .queryParam("video_source", videoSource)
                        .queryParam("interval_sec", intervalSec)
                        .queryParam("conf_threshold", confThreshold)
                        .queryParam("max_frames", maxFrames)
                        .build())
                .retrieve()
                .bodyToMono(Map.class);
    }
}