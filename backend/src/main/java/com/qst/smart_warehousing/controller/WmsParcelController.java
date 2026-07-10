package com.qst.smart_warehousing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.smart_warehousing.DTO.ParcelInboundDTO;
import com.qst.smart_warehousing.DTO.ParcelMoveDTO;
import com.qst.smart_warehousing.DTO.ParcelOutboundDTO;
import com.qst.smart_warehousing.entity.Result;
import com.qst.smart_warehousing.entity.WmsParcel;
import com.qst.smart_warehousing.service.WmsParcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "包裹主档案管理")
@RestController
@RequestMapping("/wms/parcel")
public class WmsParcelController {

    @Autowired
    private WmsParcelService parcelService;

    @Operation(summary = "录入新包裹(进线捕获)")
    @PostMapping
    public Result<?> add(@RequestBody WmsParcel parcel) {
        // 计算体积存储：长 * 宽 * 高
        if (parcel.getLength() != null && parcel.getWidth() != null && parcel.getHeight() != null) {
            parcel.setVolume(parcel.getLength().multiply(parcel.getWidth()).multiply(parcel.getHeight()));
        }
        boolean saved = parcelService.save(parcel);
        return saved ? Result.ok() : Result.error(500, "包裹档案建立失败");
    }

    @Operation(summary = "注销/删除包裹")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean removed = parcelService.removeById(id);
        return removed ? Result.ok() : Result.error(500, "包裹档案删除失败");
    }

    @Operation(summary = "更新包裹状态/属性")
    @PutMapping
    public Result<?> update(@RequestBody WmsParcel parcel) {
        if (parcel.getLength() != null && parcel.getWidth() != null && parcel.getHeight() != null) {
            parcel.setVolume(parcel.getLength().multiply(parcel.getWidth()).multiply(parcel.getHeight()));
        }
        boolean updated = parcelService.updateById(parcel);
        return updated ? Result.ok() : Result.error(500, "包裹档案修改失败");
    }

    @Operation(summary = "精确定位：通过条码查询包裹详情")
    @GetMapping("/barcode/{barcode}")
    public Result<WmsParcel> getByBarcode(@PathVariable String barcode) {
        WmsParcel parcel = parcelService.getOne(
                new LambdaQueryWrapper<WmsParcel>().eq(WmsParcel::getBarcode, barcode)
        );
        return parcel != null ? Result.ok(parcel) : Result.error(404, "未找到该条码包裹");
    }

    @Operation(summary = "分页模糊检索包裹列表")
    @GetMapping("/page")
    public Result<Page<WmsParcel>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer isDamaged) {

        Page<WmsParcel> pageParam = new Page<>(current, size);
        LambdaQueryWrapper<WmsParcel> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.like(barcode != null, WmsParcel::getBarcode, barcode)
                .eq(status != null, WmsParcel::getStatus, status)
                .eq(isDamaged != null, WmsParcel::getIsDamaged, isDamaged)
                .orderByDesc(WmsParcel::getCreateTime);

        Page<WmsParcel> resultPage = parcelService.page(pageParam, queryWrapper);
        return Result.ok(resultPage);
    }

    @Operation(summary = "业务流程：包裹确认入库落位")
    @PostMapping("/inbound")
    public Result<?> inbound(@RequestBody @Valid ParcelInboundDTO dto) { // <-- 核心变化：使用DTO并开启校验
        try {
            // 从 DTO 中干净地抽取参数传递给 Service
            boolean success = parcelService.inbound(dto.getBarcode(), dto.getTargetSlotId());
            return success ? Result.ok("包裹入库落位成功") : Result.error(500, "入库失败");
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "业务流程：包裹移库（换货位）")
    @PostMapping("/move")
    public Result<?> moveSlot(@RequestBody @Valid ParcelMoveDTO dto) { // <-- 核心变化
        try {
            boolean success = parcelService.moveSlot(dto.getBarcode(), dto.getDestSlotId());
            return success ? Result.ok("库内移位调度成功") : Result.error(500, "移库失败");
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "业务流程：包裹下架确认出库")
    @PostMapping("/outbound")
    public Result<?> outbound(@RequestBody @Valid ParcelOutboundDTO dto) {
        try {
            boolean success = parcelService.outbound(dto.getBarcode());
            return success ? Result.ok("包裹出库下架成功") : Result.error(500, "出库失败");
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
