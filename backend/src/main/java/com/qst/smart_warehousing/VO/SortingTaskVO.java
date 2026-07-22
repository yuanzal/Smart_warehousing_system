package com.qst.smart_warehousing.VO;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分拣任务列表/详情返回对象
 */
@Data
public class SortingTaskVO {
    private Long taskId;
    private String taskCode;
    private Long parcelId;
    private String barcode;
    private Integer sourceStationId;
    private Long targetSlotId;
    private String targetSlotCode;
    private Long assignedAgvId;
    private String assignedAgvCode;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}