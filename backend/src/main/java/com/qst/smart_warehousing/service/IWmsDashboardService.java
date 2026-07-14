package com.qst.smart_warehousing.service;

import com.qst.smart_warehousing.VO.WmsDashboardOverviewVO;

public interface IWmsDashboardService {
    // 获取全局运营看板一键式聚合数据
    WmsDashboardOverviewVO getOverviewData();
}