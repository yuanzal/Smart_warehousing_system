package com.qst.smart_warehousing.service;

import org.springframework.transaction.annotation.Transactional;

public interface IWmsSortingTaskService {
    boolean calculateRouteAndBalance(Long taskId, Integer projectId, Integer tenantId);

    @Transactional
    boolean updateAgvLocation(Long agvId, Double currentX, Double currentY, Integer batteryLevel);

    @Transactional
    boolean handleTaskCallback(Long taskId, Long agvId, Integer statusCallback, String errorMessage);
}
