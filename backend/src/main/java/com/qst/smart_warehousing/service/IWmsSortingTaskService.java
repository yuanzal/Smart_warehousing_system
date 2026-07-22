package com.qst.smart_warehousing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qst.smart_warehousing.DTO.SortingTaskQueryDTO;
import com.qst.smart_warehousing.VO.SortingTaskVO;
import com.qst.smart_warehousing.VO.TaskOverviewVO;
import org.springframework.transaction.annotation.Transactional;

public interface IWmsSortingTaskService {
    boolean calculateRouteAndBalance(Long taskId, Integer projectId, Integer tenantId);

    @Transactional
    boolean updateAgvLocation(Long agvId, Double currentX, Double currentY, Integer batteryLevel);

    @Transactional
    boolean handleTaskCallback(Long taskId, Long agvId, Integer statusCallback, String errorMessage);

    /**
     * 分页查询任务列表
     */
    IPage<SortingTaskVO> getTaskPage(SortingTaskQueryDTO queryDTO);

    /**
     * 查询今日任务概览统计
     */
    TaskOverviewVO getTodayOverview();

    /**
     * 重新调度异常任务
     */
    boolean retryTask(Long taskId);

    /**
     * 紧急拦截终止任务
     */
    boolean cancelTask(Long taskId);
}
