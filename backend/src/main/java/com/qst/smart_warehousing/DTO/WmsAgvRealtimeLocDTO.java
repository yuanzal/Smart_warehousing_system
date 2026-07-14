package com.qst.smart_warehousing.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * AGV 实时状态与坐标 DTO
 */
@Schema(name = "AGV实时状态与坐标传输对象")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WmsAgvRealtimeLocDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long agvId;
    private Double currentX;
    private Double currentY;
    private Integer batteryLevel;
    private Long timestamp; // 上报的时间戳
}