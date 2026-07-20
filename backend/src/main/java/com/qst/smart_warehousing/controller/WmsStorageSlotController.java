package com.qst.smart_warehousing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.smart_warehousing.entity.AdminUser;
import com.qst.smart_warehousing.entity.Result;
import com.qst.smart_warehousing.entity.WmsStorageSlot;
import com.qst.smart_warehousing.service.WmsStorageSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "货位基础档案管理")
@RestController
@RequestMapping("/wms/storage-slot") // 修正为驼峰，匹配前端请求地址
public class WmsStorageSlotController {

    @Autowired
    private WmsStorageSlotService storageSlotService;

    // 获取当前登录用户工具方法
    private AdminUser getLoginUser() {
        return (AdminUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Operation(summary = "新增货位")
    @PostMapping("/add") // 匹配前端 /wms/storageSlot/add
    public Result<?> add(@RequestBody WmsStorageSlot storageSlot) {
        AdminUser loginUser = getLoginUser();
        // 多租户权限控制：非平台管理员强制覆盖租户ID
        if (loginUser.getTenantId() != 0) {
            storageSlot.setTenantId(loginUser.getTenantId());
        }
        // 校验同租户货位编码重复
        LambdaQueryWrapper<WmsStorageSlot> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(WmsStorageSlot::getSlotCode, storageSlot.getSlotCode())
                .eq(WmsStorageSlot::getTenantId, storageSlot.getTenantId());
        long count = storageSlotService.count(existWrapper);
        if (count > 0) {
            return Result.error(400, "当前租户下该货位编码已存在");
        }
        boolean saved = storageSlotService.save(storageSlot);
        return saved ? Result.ok() : Result.error(500, "货位添加失败");
    }

    @Operation(summary = "删除货位")
    @DeleteMapping("/{slotId}")
    public Result<?> delete(@PathVariable Long slotId) {
        AdminUser loginUser = getLoginUser();
        // 1. 校验货位归属，普通租户只能删自己租户数据
        WmsStorageSlot slot = storageSlotService.getById(slotId);
        if (slot == null) return Result.error(404, "货位不存在");
        if (loginUser.getTenantId() != 0 && !slot.getTenantId().equals(loginUser.getTenantId())) {
            return Result.error(403, "无权删除其他租户货位");
        }
        // 2. 业务校验：已占用包裹禁止删除（service层实现包裹关联判断）
        boolean canDelete = storageSlotService.checkSlotNoParcel(slotId);
        if (!canDelete) {
            return Result.error(400, "该货位仍绑定包裹，无法删除");
        }
        boolean removed = storageSlotService.removeById(slotId);
        return removed ? Result.ok() : Result.error(500, "货位删除失败");
    }

    @Operation(summary = "修改货位全量信息")
    @PutMapping
    public Result<?> update(@RequestBody WmsStorageSlot storageSlot) {
        AdminUser loginUser = getLoginUser();
        WmsStorageSlot oldSlot = storageSlotService.getById(storageSlot.getSlotId());
        if (oldSlot == null) return Result.error(404, "货位不存在");
        // 租户越权拦截
        if (loginUser.getTenantId() != 0 && !oldSlot.getTenantId().equals(loginUser.getTenantId())) {
            return Result.error(403, "无权修改其他租户货位");
        }
        // 普通租户禁止修改租户ID
        if (loginUser.getTenantId() != 0) {
            storageSlot.setTenantId(oldSlot.getTenantId());
        }
        boolean updated = storageSlotService.updateById(storageSlot);
        return updated ? Result.ok() : Result.error(500, "货位更新失败");
    }

    @Operation(summary = "仅更新货位状态（故障/空闲切换）")
    @PutMapping("/status")
    public Result<?> updateStatus(@RequestParam Long slotId, @RequestParam Integer status) {
        AdminUser loginUser = getLoginUser();
        WmsStorageSlot slot = storageSlotService.getById(slotId);
        if (slot == null) return Result.error(404, "货位不存在");
        if (loginUser.getTenantId() != 0 && !slot.getTenantId().equals(loginUser.getTenantId())) {
            return Result.error(403, "无权操作其他租户货位");
        }
        WmsStorageSlot update = new WmsStorageSlot();
        update.setSlotId(slotId);
        update.setStatus(status);
        boolean res = storageSlotService.updateById(update);
        return res ? Result.ok() : Result.error(500, "状态更新失败");
    }

    @Operation(summary = "根据ID获取单个货位详情(用于大屏弹窗)")
    @GetMapping("/{slotId}")
    public Result<WmsStorageSlot> getById(@PathVariable Long slotId) {
        WmsStorageSlot slot = storageSlotService.getById(slotId);
        return slot != null ? Result.ok(slot) : Result.error(404, "未找到该货位");
    }

    @Operation(summary = "分页条件查询货位列表（带租户隔离、库区筛选）")
    @GetMapping("/page")
    public Result<Page<WmsStorageSlot>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String slotCode,
            @RequestParam(required = false) String zoneName, // 新增库区名称筛选
            @RequestParam(required = false) Integer status) {

        AdminUser loginUser = getLoginUser();
        Page<WmsStorageSlot> pageParam = new Page<>(current, size);
        LambdaQueryWrapper<WmsStorageSlot> queryWrapper = new LambdaQueryWrapper<>();

        // 多租户隔离：普通租户强制只查自己租户数据
        if (loginUser.getTenantId() != 0) {
            queryWrapper.eq(WmsStorageSlot::getTenantId, loginUser.getTenantId());
        }
        // 动态条件拼装
        queryWrapper.like(slotCode != null, WmsStorageSlot::getSlotCode, slotCode)
                .like(zoneName != null, WmsStorageSlot::getZoneName, zoneName) // 库区模糊查询
                .eq(status != null, WmsStorageSlot::getStatus, status)
                .orderByAsc(WmsStorageSlot::getSlotCode);

        Page<WmsStorageSlot> resultPage = storageSlotService.page(pageParam, queryWrapper);
        // Service层补充联表 parcelCode、currentWeight 扩展字段
        storageSlotService.fillParcelWeightInfo(resultPage.getRecords());
        return Result.ok(resultPage);
    }
}