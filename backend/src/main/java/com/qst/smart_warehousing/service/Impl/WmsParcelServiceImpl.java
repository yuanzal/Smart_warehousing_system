package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smart_warehousing.entity.WmsInventoryLog;
import com.qst.smart_warehousing.entity.WmsParcel;
import com.qst.smart_warehousing.entity.WmsStorageSlot;
import com.qst.smart_warehousing.mapper.WmsParcelMapper;
import com.qst.smart_warehousing.service.WmsInventoryLogService;
import com.qst.smart_warehousing.service.WmsParcelService;
import com.qst.smart_warehousing.service.WmsStorageSlotService;
import com.qst.smart_warehousing.entity.AdminUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmsParcelServiceImpl extends ServiceImpl<WmsParcelMapper, WmsParcel> implements WmsParcelService {
    @Autowired
    private WmsStorageSlotService storageSlotService;

    @Autowired
    private WmsInventoryLogService wmsInventoryLogService;

    /**
     * 获取当前登录用户的ID（从Spring Security安全口袋里掏）
     */
    private Long getCurrentUserId() {
        try {
            AdminUser loginUser = (AdminUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return loginUser.getUserId();
        } catch (Exception e) {
            return 1L; // 边缘设备或测试未登录时的保底系统管理员ID
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean inbound(String barcode, Long targetSlotId) {
        // 1. 严格校验：包裹是否存在
        WmsParcel parcel = this.getOne(new LambdaQueryWrapper<WmsParcel>().eq(WmsParcel::getBarcode, barcode));
        if (parcel == null) {
            throw new RuntimeException("入库失败：未找到条码为 " + barcode + " 的包裹档案");
        }
        if (parcel.getStatus() == 4) {
            throw new RuntimeException("入库失败：该包裹已经是“已上架入库”状态");
        }

        // 2. 严格校验：目标货位是否存在且可用
        WmsStorageSlot slot = storageSlotService.getById(targetSlotId);
        if (slot == null) {
            throw new RuntimeException("入库失败：目标货位不存在");
        }
        if (slot.getStatus() == 1) {
            throw new RuntimeException("入库失败：目标货位 [" + slot.getSlotCode() + "] 已被其他包裹占用");
        }
        if (slot.getStatus() == 3) {
            throw new RuntimeException("入库失败：目标货位正在故障维修中");
        }

        // 3. 更新货位状态 -> 1 (已占用)
        slot.setStatus(1);
        storageSlotService.updateById(slot);

        // 4. 更新包裹状态 -> 4 (已上架入库)
        parcel.setStatus(4);
        parcel.setCurrentSlotId(targetSlotId);
        this.updateById(parcel);

        // 5. 【新增】：自动生成入库流水
        WmsInventoryLog log = new WmsInventoryLog();
        log.setParcelId(parcel.getParcelId());
        log.setBarcode(barcode);
        log.setActionType(1); // 1-入库
        log.setFromSlotId(null);
        log.setToSlotId(targetSlotId);
        log.setOperatorId(getCurrentUserId());
        log.setProjectId(parcel.getProjectId());
        log.setTenantId(parcel.getTenantId());

        return wmsInventoryLogService.save(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean outbound(String barcode) {
        // 1. 校验包裹
        WmsParcel parcel = this.getOne(new LambdaQueryWrapper<WmsParcel>().eq(WmsParcel::getBarcode, barcode));
        if (parcel == null) {
            throw new RuntimeException("出库失败：未找到该包裹");
        }
        if (parcel.getStatus() == 5) {
            throw new RuntimeException("出库失败：该包裹此前已完成出库");
        }

        // 2. 释放原货位资源
        Long oldSlotId = parcel.getCurrentSlotId();
        if (oldSlotId != null) {
            WmsStorageSlot oldSlot = storageSlotService.getById(oldSlotId);
            if (oldSlot != null) {
                oldSlot.setStatus(0); // 恢复为 0 (空闲)
                storageSlotService.updateById(oldSlot);
            }
        }

        // 3. 更新包裹状态 -> 5 (已出库)，清空当前货位绑定
        parcel.setStatus(5);
        parcel.setCurrentSlotId(null);

        this.updateById(parcel);

        // 4. 【新增】：自动生成出库流水
        WmsInventoryLog log = new WmsInventoryLog();
        log.setBarcode(barcode);
        log.setParcelId(parcel.getParcelId());
        log.setActionType(2); // 2-出库
        log.setFromSlotId(oldSlotId); // 记录从哪个货位出来的
        log.setToSlotId(null);
        log.setOperatorId(getCurrentUserId());
        log.setProjectId(parcel.getProjectId());
        log.setTenantId(parcel.getTenantId());

        return wmsInventoryLogService.save(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveSlot(String barcode, Long destSlotId) {
        // 1. 校验包裹及当前位置
        WmsParcel parcel = this.getOne(new LambdaQueryWrapper<WmsParcel>().eq(WmsParcel::getBarcode, barcode));
        if (parcel == null) {
            throw new RuntimeException("移库失败：未找到该包裹");
        }
        if (parcel.getCurrentSlotId() == null) {
            throw new RuntimeException("移库失败：该包裹目前未分配任何货位，无法执行库内移位");
        }
        if (parcel.getCurrentSlotId().equals(destSlotId)) {
            throw new RuntimeException("移库失败：目标货位与当前所在货位相同");
        }

        // 2. 校验新目标货位
        WmsStorageSlot destSlot = storageSlotService.getById(destSlotId);
        if (destSlot == null || destSlot.getStatus() != 0) {
            throw new RuntimeException("移库失败：目标新货位不存在或非空闲状态");
        }

        // 3. 释放老货位
        WmsStorageSlot sourceSlot = storageSlotService.getById(parcel.getCurrentSlotId());
        if (sourceSlot != null) {
            sourceSlot.setStatus(0); // 变为空闲
            storageSlotService.updateById(sourceSlot);
        }

        // 4. 占用新货位
        destSlot.setStatus(1); // 变为占用
        storageSlotService.updateById(destSlot);

        // 5. 更新包裹的货位指向
        parcel.setCurrentSlotId(destSlotId);

        this.updateById(parcel);

        // 6. 【新增】：自动生成移库流水
        WmsInventoryLog log = new WmsInventoryLog();
        log.setBarcode(barcode);
        log.setParcelId(parcel.getParcelId());
        log.setActionType(3); // 3-移库
        log.setFromSlotId(sourceSlot.getSlotId()); // 始发地
        log.setToSlotId(destSlotId);     // 目的地
        log.setOperatorId(getCurrentUserId());
        log.setProjectId(parcel.getProjectId());
        log.setTenantId(parcel.getTenantId());

        return wmsInventoryLogService.save(log);
    }
}
