package com.qst.smart_warehousing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qst.smart_warehousing.DTO.DeviceDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceCacheService {

    private static final String DEVICE_STATUS_KEY = "device:status";  // Hash: deviceId -> JSON
    private static final String AGV_POSITION_KEY = "agv:positions";   // Hash: agvId -> {x, y, z}

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 更新设备缓存
     */
    public void updateDeviceStatus(DeviceDataDTO dto) throws Exception {
        System.out.println("=== updateDeviceStatus 被调用 ===");
        System.out.println("deviceId: " + dto.getDeviceId());
        System.out.println("deviceType: " + dto.getDeviceType());

        String deviceId = dto.getDeviceId();
        String json = objectMapper.writeValueAsString(dto);
        System.out.println("序列化后的 JSON: " + json);

        redisTemplate.opsForHash().put(DEVICE_STATUS_KEY, deviceId, json);
        redisTemplate.expire(DEVICE_STATUS_KEY, 10, TimeUnit.MINUTES);
        System.out.println("已写入 device:status, key=" + deviceId);

        // 如果是 AGV，同时更新 AGV 位置缓存（方便 3D 场景单独查询）
        if (dto.getDeviceType() != null && dto.getDeviceType() == 2) {
            Map<String, Object> data = dto.getData();
            if (data.containsKey("x") && data.containsKey("z")) {
                Map<String, Object> pos = new HashMap<>();
                pos.put("x", data.get("x"));
                pos.put("z", data.get("z"));
                pos.put("angle", data.getOrDefault("angle", 0.0));
                pos.put("timestamp", dto.getTimestamp());
                String posJson = objectMapper.writeValueAsString(pos);
                redisTemplate.opsForHash().put(AGV_POSITION_KEY, deviceId, posJson);
                redisTemplate.expire(AGV_POSITION_KEY, 10, TimeUnit.MINUTES);
                System.out.println("已写入 agv:positions, key=" + deviceId + ", posJson=" + posJson);
            } else {
                System.out.println("AGV 数据缺少 x 或 z 字段，不写入位置缓存");
            }
        }
        System.out.println("=== updateDeviceStatus 执行完成 ===");
    }

    /**
     * 获取所有设备最新状态
     */
    public Map<Object, Object> getAllDeviceStatus() {
        return redisTemplate.opsForHash().entries(DEVICE_STATUS_KEY);
    }

    /**
     * 获取单个设备状态
     */
    public Object getDeviceStatus(String deviceId) {
        return redisTemplate.opsForHash().get(DEVICE_STATUS_KEY, deviceId);
    }

    /**
     * 获取所有 AGV 位置
     */
    public Map<Object, Object> getAllAgvPositions() {
        return redisTemplate.opsForHash().entries(AGV_POSITION_KEY);
    }
}