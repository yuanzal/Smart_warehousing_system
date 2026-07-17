package com.qst.smart_warehousing.DTO;

import lombok.Data;
import java.util.Map;

@Data
public class DeviceDataDTO {
    private String deviceId;           // 设备唯一标识
    private Integer deviceType;        // 1-摄像头, 2-AGV, 3-传感器, 4-边缘网关
    private Long timestamp;            // 上报时间戳（毫秒）
    private Map<String, Object> data;  // 具体数据：位置坐标、温度、电量等
}