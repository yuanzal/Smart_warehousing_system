package com.qst.smart_warehousing.controller;

import com.qst.smart_warehousing.entity.WmsStorageSlot;
import com.qst.smart_warehousing.service.WmsStorageSlotService;
import com.qst.smart_warehousing.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/storage")
public class StorageSlotController {

    @Autowired
    private WmsStorageSlotService slotService;

    @GetMapping("/slots")
    public Result<List<WmsStorageSlot>> getAllSlots() {
        List<WmsStorageSlot> slots = slotService.list();
        // TODO: 前端展示缺少字段，此处用硬编码替代，后面封装DTO使用联表查询
        // ===== 填充模拟扩展数据 =====
        slots.forEach(slot -> {
            // 模拟当前载重（0~300 kg 随机）
            slot.setCurrentWeight(BigDecimal.valueOf(Math.random() * 300));
            // 模拟包裹编号（若状态为占用则生成，否则为空）
            if (slot.getStatus() == 1) {
                slot.setParcelCode("P-" + (System.currentTimeMillis() % 100000) + (int)(Math.random()*100));
            } else {
                slot.setParcelCode("");
            }
            // 模拟楼层（根据 slotId 分配 1~3 层）
            slot.setFloor((int)(slot.getSlotId() % 3) + 1);
        });
        return Result.ok(slots);
    }
}