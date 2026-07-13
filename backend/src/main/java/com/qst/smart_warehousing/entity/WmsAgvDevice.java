package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("wms_agv_device")
@Schema(title = "AGV智能小车设备状态与调度表")
public class WmsAgvDevice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long agvId;

    private String agvCode;         // AGV车辆唯一硬编码
    private String agvModel;        // AGV车辆载重及规格型号
    private Integer status;          // 车辆运行状态: 0离线, 1待命空闲, 2配送执行中, 3低电自主充电, 4故障卡死
    private Integer batteryLevel;    // 小车当前实时剩余电量百分比(0-100)
    private BigDecimal currentX;     // 3D孪生空间实时X横坐标(米)
    private BigDecimal currentY;     // 3D孪生空间实时Y纵坐标(米)
    private BigDecimal currentZ;     // 3D孪生空间实时Z垂直高坐标(米)
    private String routePathJson;    // 搬运最优路径规划拓扑节点空间逻辑坐标集合(JSON串)
    private BigDecimal loadWeight;   // 车载传感器实时测载重量(kg)
    private Integer projectId;
    private Integer tenantId;
}