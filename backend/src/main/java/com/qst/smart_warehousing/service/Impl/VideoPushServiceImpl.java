package com.qst.smart_warehousing.service.Impl;

import com.qst.smart_warehousing.config.VideoConfig;
import com.qst.smart_warehousing.DTO.VideoStreamDTO;
import com.qst.smart_warehousing.service.VideoPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VideoPushServiceImpl implements VideoPushService {
    private static final Logger logger = LoggerFactory.getLogger(VideoPushServiceImpl.class);

    @Autowired
    private VideoConfig videoConfig;

    private final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> runningMap = new ConcurrentHashMap<>();

    private static final String DEFAULT_TEST_VIDEO = "test.mp4";

    @Override
    public VideoStreamDTO startPush(String streamId, String rtspUrl) {
        if (runningMap.containsKey(streamId) && runningMap.get(streamId)) {
            logger.warn("Stream {} is already running, stopping first.", streamId);
            stopPush(streamId);
        }

        String rtmpUrl = videoConfig.getRtmpServer() + streamId;
        String hlsUrl = videoConfig.getHlsBaseUrl() + streamId + ".m3u8";

        ProcessBuilder pb;
        if (rtspUrl != null && !rtspUrl.trim().isEmpty()) {
            pb = new ProcessBuilder(
                    videoConfig.getFfmpegPath(),
                    "-re", "-i", rtspUrl,
                    "-c:v", "copy",
                    "-c:a", "aac",
                    "-f", "flv",
                    rtmpUrl
            );
        } else {
            String videoPath = new File(DEFAULT_TEST_VIDEO).getAbsolutePath();
            if (!new File(videoPath).exists()) {
                videoPath = "C:/nginx-rtmp/test.mp4";
                if (!new File(videoPath).exists()) {
                    logger.error("Test video not found: {}", videoPath);
                    throw new RuntimeException("Test video not found: " + videoPath);
                }
            }
            pb = new ProcessBuilder(
                    videoConfig.getFfmpegPath(),
                    "-re",
                    "-stream_loop", "-1",
                    "-i", videoPath,
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-g", String.valueOf(videoConfig.getHlsFragmentSeconds() * 10),
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-f", "flv",
                    rtmpUrl
            );
        }

        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            processMap.put(streamId, process);
            runningMap.put(streamId, true);

            new Thread(() -> {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.debug("[{}] {}", streamId, line);
                    }
                } catch (IOException e) {
                    logger.warn("Stream {} log reader error: {}", streamId, e.getMessage());
                }
            }).start();

            logger.info("Stream {} started, RTMP: {}, HLS: {}", streamId, rtmpUrl, hlsUrl);

            VideoStreamDTO dto = new VideoStreamDTO();
            dto.setStreamId(streamId);
            dto.setRtspUrl(rtspUrl);
            dto.setRtmpUrl(rtmpUrl);
            dto.setHlsUrl(hlsUrl);
            dto.setStatus("running");
            dto.setPid((int) process.pid());
            return dto;

        } catch (IOException e) {
            logger.error("Failed to start stream {}: {}", streamId, e.getMessage(), e);
            runningMap.put(streamId, false);
            throw new RuntimeException("Failed to start FFmpeg process: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean stopPush(String streamId) {
        Process process = processMap.get(streamId);
        if (process == null) {
            runningMap.put(streamId, false);
            logger.warn("Stream {} not found", streamId);
            return false;
        }

        process.destroy();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for stream {} to stop", streamId);
        }
        processMap.remove(streamId);
        runningMap.put(streamId, false);
        logger.info("Stream {} stopped", streamId);
        return true;
    }

    @Override
    public VideoStreamDTO getStatus(String streamId) {
        VideoStreamDTO dto = new VideoStreamDTO();
        dto.setStreamId(streamId);
        boolean running = runningMap.getOrDefault(streamId, false);
        dto.setStatus(running ? "running" : "stopped");
        Process process = processMap.get(streamId);
        if (process != null) {
            dto.setPid((int) process.pid());
        }
        dto.setRtmpUrl(videoConfig.getRtmpServer() + streamId);
        dto.setHlsUrl(videoConfig.getHlsBaseUrl() + streamId + ".m3u8");
        return dto;
    }
}