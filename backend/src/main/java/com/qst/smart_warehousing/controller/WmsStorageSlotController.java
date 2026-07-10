package com.qst.smart_warehousing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.qst.smart_warehousing.entity.Result;
import com.qst.smart_warehousing.entity.WmsStorageSlot;
import com.qst.smart_warehousing.service.WmsStorageSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "货位基础档案管理")
@RestController
@RequestMapping("/wms/storage-slot")
public class WmsStorageSlotController {

    @Autowired
    private WmsStorageSlotService storageSlotService;

    @Operation(summary = "新增货位")
    @PostMapping
    public Result<?> add(@RequestBody WmsStorageSlot storageSlot) {
        boolean saved = storageSlotService.save(storageSlot);
        return saved ? Result.ok() : Result.error(500, "货位添加失败");
    }

    @Operation(summary = "删除货位")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean removed = storageSlotService.removeById(id);
        return removed ? Result.ok() : Result.error(500, "货位删除失败");
    }

    @Operation(summary = "修改货位信息")
    @PutMapping
    public Result<?> update(@RequestBody WmsStorageSlot storageSlot) {
        boolean updated = storageSlotService.updateById(storageSlot);
        return updated ? Result.ok() : Result.error(500, "货位更新失败");
    }

    @Operation(summary = "根据ID获取单个货位详情(用于大屏弹窗)")
    @GetMapping("/{id}")
    public Result<WmsStorageSlot> getById(@PathVariable Long id) {
        WmsStorageSlot slot = storageSlotService.getById(id);
        return slot != null ? Result.ok(slot) : Result.error(404, "未找到该货位");
    }

    @Operation(summary = "分页条件查询货位列表")
    @GetMapping("/page")
    public Result<Page<WmsStorageSlot>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String slotCode,
            @RequestParam(required = false) Integer status) {

        Page<WmsStorageSlot> pageParam = new Page<>(current, size);
        LambdaQueryWrapper<WmsStorageSlot> queryWrapper = new LambdaQueryWrapper<>();

        // 动态条件拼装
        queryWrapper.like(slotCode != null, WmsStorageSlot::getSlotCode, slotCode)
                .eq(status != null, WmsStorageSlot::getStatus, status)
                .orderByAsc(WmsStorageSlot::getSlotCode);

        Page<WmsStorageSlot> resultPage = storageSlotService.page(pageParam, queryWrapper);
        return Result.ok(resultPage);
    }
}
