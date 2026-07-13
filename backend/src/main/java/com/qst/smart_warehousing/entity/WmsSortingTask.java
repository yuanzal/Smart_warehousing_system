package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wms_sorting_task")
@Schema(title = "智能分拣调度任务表")
public class WmsSortingTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long taskId;

    private String taskCode;          // 分拣调度唯一任务单号
    private Long parcelId;            // 关联包裹档案ID
    private String barcode;           // 包裹条码
    private Long sourceStationId;     // 起始进线分拣滑道/档位ID
    private Long targetSlotId;        // 智能中台算法动态规划推荐的最佳目的货位ID
    private Long assignedAgvId;       // 中台匹配调度承接此搬运任务的AGV智能小车ID
    private Integer status;           // 分拣状态: 1任务创建, 2路由计算完毕, 3AGV无缝转运中, 4机械臂已成功分拣入库, 5中途异常拦截终止
    private LocalDateTime createTime; // 生成时间
    private LocalDateTime startTime;  // 开始执行时间
    private LocalDateTime endTime;    // 核验落位圆满完成时间
    private Integer projectId;
    private Integer tenantId;
}