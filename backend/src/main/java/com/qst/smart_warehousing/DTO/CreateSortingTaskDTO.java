package com.qst.smart_warehousing.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建分拣任务请求参数")
public class CreateSortingTaskDTO {
    private Long parcelId;          // 对应 wms_parcel.parcel_id
    private String barcode;         // 包裹条码
    private Long sourceStationId;   // 进线滑道/档位设备ID (对应 wms_edge_device.device_id)
    private Integer projectId;
    private Integer tenantId;
}
