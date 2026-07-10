package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wms_inventory_log")
@Schema(title = "包裹状态变更表")
public class WmsInventoryLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Long parcelId;
    private String barcode;        // 包裹条码
    private Integer actionType;    // 操作类型：1-入库，2-出库，3-移库
    private Long fromSlotId;     // 源货位ID（出库、移库时记录）
    private Long toSlotId;       // 目标货位ID（入库、移库时记录）
    private Long operatorId;       // 操作员ID

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Integer projectId;
    private Integer tenantId;
}