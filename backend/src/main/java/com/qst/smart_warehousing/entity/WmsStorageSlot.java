package com.qst.smart_warehousing.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wms_storage_slot")
@Schema(title = "货位仓位信息表")
public class WmsStorageSlot implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "slot_id", type = IdType.AUTO)
    @Schema(title = "货位主键")
    private Long slotId;

    @Schema(title = "货位编码(如: A-01-03)")
    private String slotCode;

    @Schema(title = "区域名称(如: A区冷链、B区重货)")
    private String zoneName;

    @Schema(title = "货架行")
    private Integer shelfRow;

    @Schema(title = "货架列")
    private Integer shelfColumn;

    @Schema(title = "货架层")
    private Integer shelfLayer;

    @Schema(title = "Three.js 孪生大屏X空间物理坐标(米)")
    private BigDecimal xCoordinate;

    @Schema(title = "Three.js 孪生大屏Y空间物理坐标(米)")
    private BigDecimal yCoordinate;

    @Schema(title = "Three.js 孪生大屏Z空间物理坐标(米)")
    private BigDecimal zCoordinate;

    @Schema(title = "货位状态: 0空闲, 1已占用, 2锁定(分拣任务预定), 3故障维修")
    private Integer status;

    @Schema(title = "最大承重(kg)")
    private BigDecimal maxWeight;

    @Schema(title = "项目ID")
    private Integer projectId;

    @Schema(title = "租户ID")
    private Integer tenantId;
}