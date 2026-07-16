package com.qst.smart_warehousing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "video")
public class VideoConfig {
    private String ffmpegPath = "ffmpeg";
    private String rtmpServer = "rtmp://localhost:1935/live/";
    private String hlsBaseUrl = "http://localhost:8081/hls/";
    private String hlsDir = "C:/nginx-rtmp/html/hls/";
    private int hlsFragmentSeconds = 6;

    public String getFfmpegPath() { return ffmpegPath; }
    public void setFfmpegPath(String ffmpegPath) { this.ffmpegPath = ffmpegPath; }
    public String getRtmpServer() { return rtmpServer; }
    public void setRtmpServer(String rtmpServer) { this.rtmpServer = rtmpServer; }
    public String getHlsBaseUrl() { return hlsBaseUrl; }
    public void setHlsBaseUrl(String hlsBaseUrl) { this.hlsBaseUrl = hlsBaseUrl; }
    public String getHlsDir() { return hlsDir; }
    public void setHlsDir(String hlsDir) { this.hlsDir = hlsDir; }
    public int getHlsFragmentSeconds() { return hlsFragmentSeconds; }
    public void setHlsFragmentSeconds(int hlsFragmentSeconds) { this.hlsFragmentSeconds = hlsFragmentSeconds; }
}