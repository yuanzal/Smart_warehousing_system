package com.qst.smart_warehousing.service;

import com.qst.smart_warehousing.DTO.VideoStreamDTO;

import java.util.Map;

public interface VideoPushService {
    VideoStreamDTO startPush(String streamId, String rtspUrl);
    boolean stopPush(String streamId);
    VideoStreamDTO getStatus(String streamId);
    Map<String, VideoStreamDTO> getAllStreams();
}