package com.qst.smart_warehousing.service;

public interface IWmsSortingTaskService {
    boolean calculateRouteAndBalance(Long taskId, Integer projectId, Integer tenantId);
}
