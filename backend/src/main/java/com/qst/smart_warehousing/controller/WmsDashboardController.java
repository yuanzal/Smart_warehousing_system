package com.qst.smart_warehousing.controller;

import com.qst.smart_warehousing.VO.WmsDashboardOverviewVO;
import com.qst.smart_warehousing.entity.Result; // 💡 引入你们项目统一定义的统一返回体
import com.qst.smart_warehousing.service.IWmsDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.annotation.Resource;

@Tag(name = "WMS智能运营控制塔-数字化看板")
@RestController
@RequestMapping("/wms/dashboard")
public class WmsDashboardController {

    @Resource
    private IWmsDashboardService dashboardService;

    @Operation(summary = "中台驾驶舱一键抓取：获取全局聚合KPI与图表结构体数据")
    @GetMapping("/overview")
    public Result<WmsDashboardOverviewVO> getDashboardOverview() {
        // 💡 核心修复：不用 ResponseEntity，改用 Result.ok() 把业务对象包裹起来
        // 这样吐给前端的 JSON 就会自带 {"code": 200, "msg": "操作成功", "data": {...}}
        WmsDashboardOverviewVO data = dashboardService.getOverviewData();
        return Result.ok(data);
    }
}