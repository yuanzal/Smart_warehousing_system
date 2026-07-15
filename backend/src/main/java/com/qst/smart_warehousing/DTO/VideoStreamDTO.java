package com.qst.smart_warehousing.DTO;

public class VideoStreamDTO {
    private String streamId;
    private String rtspUrl;
    private String rtmpUrl;
    private String hlsUrl;
    private String status;
    private Integer pid;

    public String getStreamId() { return streamId; }
    public void setStreamId(String streamId) { this.streamId = streamId; }
    public String getRtspUrl() { return rtspUrl; }
    public void setRtspUrl(String rtspUrl) { this.rtspUrl = rtspUrl; }
    public String getRtmpUrl() { return rtmpUrl; }
    public void setRtmpUrl(String rtmpUrl) { this.rtmpUrl = rtmpUrl; }
    public String getHlsUrl() { return hlsUrl; }
    public void setHlsUrl(String hlsUrl) { this.hlsUrl = hlsUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPid() { return pid; }
    public void setPid(Integer pid) { this.pid = pid; }
}