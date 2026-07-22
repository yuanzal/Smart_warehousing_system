package com.qst.smart_warehousing.VO;

import lombok.Data;

/**
 * 今日分拣任务概览指标
 */
@Data
public class TaskOverviewVO {
    /**
     * 今日总任务量
     */
    private Integer totalCount;
    /**
     * 转运中任务数（status=3）
     */
    private Integer transportingCount;
    /**
     * 已完成入库数（status=4）
     */
    private Integer completedCount;
    /**
     * 异常终止数（status=5）
     */
    private Integer exceptionCount;
}