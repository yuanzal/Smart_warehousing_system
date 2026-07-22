package com.qst.smart_warehousing.DTO;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分拣任务分页查询条件
 */
@Data
public class SortingTaskQueryDTO {
    /**
     * 关键词：任务单号/包裹条码模糊搜索
     */
    private String keyword;

    /**
     * 任务状态：null查询全部
     */
    private Integer status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;
}