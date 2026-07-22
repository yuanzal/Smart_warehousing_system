package com.qst.smart_warehousing.service.Impl;

import com.qst.smart_warehousing.DTO.VideoStreamDTO;
import com.qst.smart_warehousing.service.VideoPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VideoPushServiceImpl implements VideoPushService {
    private static final Logger logger = LoggerFactory.getLogger(VideoPushServiceImpl.class);

    // ===== 配置项（从 application.yml 注入） =====
    @Value("${video.ffmpeg.path:C:/ffmpeg/ffmpeg-8.1.2-essentials_build/bin/ffmpeg.exe}")
    private String ffmpegPath;

    @Value("${video.test.source:C:/Users/12256/Downloads/Test Jellyfin 1080p AVC 3M.mp4}")
    private String testVideoSource;

    // HLS 输出目录（必须与 Nginx 的 hls_path 一致）
    @Value("${video.hls.output-dir:C:/nginx-rtmp/html/hls}")
    private String hlsDir;

    @Value("${video.hls.prefix:http://localhost:8081/hls/}")
    private String hlsPrefix;

    // 进程管理
    private final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> runningMap = new ConcurrentHashMap<>();

    @Override
    public VideoStreamDTO startPush(String streamId, String rtspUrl) {
        logger.info("启动 HLS 生成: streamId={}, rtspUrl={}", streamId, rtspUrl);

        // 如果已有进程，先停止
        if (runningMap.containsKey(streamId) && runningMap.get(streamId)) {
            stopPush(streamId);
        }

        // 确定输入源
        String inputSource;
        if (rtspUrl != null && !rtspUrl.trim().isEmpty()) {
            inputSource = rtspUrl;
        } else {
            // 检查测试视频是否存在，不存在则使用 lavfi 测试源
            File testFile = new File(testVideoSource);
            if (testFile.exists()) {
                inputSource = testVideoSource;
            } else {
                logger.warn("测试视频不存在，使用 lavfi 测试源");
                inputSource = "lavfi=testsrc=size=1280x720:rate=30";
            }
        }

        // 输出路径
        String outputPath = hlsDir + "/" + streamId + ".m3u8";
        // 确保目录存在
        new File(hlsDir).mkdirs();

        // 构建 FFmpeg 命令（直接生成 HLS）
        ProcessBuilder pb;
        if (inputSource.startsWith("lavfi")) {
            // 使用 lavfi 测试源
            pb = new ProcessBuilder(
                    ffmpegPath,
                    "-re",
                    "-f", "lavfi",
                    "-i", "testsrc=size=1280x720:rate=30",
                    "-c:v", "libx264",
                    "-c:a", "aac",
                    "-f", "hls",
                    "-hls_time", "6",
                    "-hls_list_size", "10",
                    "-hls_flags", "delete_segments",
                    outputPath
            );
        } else {
            // 使用视频文件（本地或 RTSP）
            pb = new ProcessBuilder(
                    ffmpegPath,
                    "-re",
                    "-stream_loop", "-1",      // 循环播放
                    "-i", inputSource,
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-f", "hls",
                    "-hls_time", "6",
                    "-hls_list_size", "10",
                    "-hls_flags", "delete_segments",
                    outputPath
            );
        }

        pb.redirectErrorStream(true);
        // 设置工作目录为 FFmpeg 所在目录（避免路径问题）
        pb.directory(new File(ffmpegPath).getParentFile());

        logger.info("FFmpeg 命令: {}", String.join(" ", pb.command()));

        try {
            Process process = pb.start();
            processMap.put(streamId, process);
            runningMap.put(streamId, true);
            logger.info("HLS 生成进程已启动, PID: {}", process.pid());

            // 日志读取线程（便于调试）
            new Thread(() -> {
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.debug("[{}] {}", streamId, line);
                    }
                } catch (IOException e) {
                    logger.warn("读取进程输出失败: {}", e.getMessage());
                }
            }).start();

            // 监控进程退出
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    logger.info("HLS 生成进程退出, streamId={}, exitCode={}", streamId, exitCode);
                    if (exitCode != 0) {
                        runningMap.put(streamId, false);
                        processMap.remove(streamId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("等待进程退出被中断");
                }
            }).start();

            // 构建返回 DTO
            VideoStreamDTO dto = new VideoStreamDTO();
            dto.setStreamId(streamId);
            dto.setRtspUrl(rtspUrl);
            dto.setRtmpUrl(null);                 // 不再使用 RTMP
            dto.setHlsUrl("http://localhost:8081/hls/" + streamId + ".m3u8");
            dto.setStatus("running");
            dto.setPid((int) process.pid());
            return dto;

        } catch (IOException e) {
            logger.error("启动 HLS 生成失败: {}", e.getMessage(), e);
            runningMap.put(streamId, false);
            throw new RuntimeException("启动 FFmpeg 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean stopPush(String streamId) {
        Process process = processMap.get(streamId);
        if (process == null) {
            runningMap.put(streamId, false);
            logger.warn("流 {} 未在运行", streamId);
            return false;
        }
        process.destroy();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("等待进程终止被中断");
        }
        processMap.remove(streamId);
        runningMap.put(streamId, false);
        logger.info("流 {} 已停止", streamId);
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
        dto.setRtmpUrl(null);
        dto.setHlsUrl("http://localhost:8081/hls/" + streamId + ".m3u8");
        return dto;
    }

    @Override
    public Map<String, VideoStreamDTO> getAllStreams() {
        // 如需全量状态，可遍历 runningMap 返回
        return Map.of();
    }
}