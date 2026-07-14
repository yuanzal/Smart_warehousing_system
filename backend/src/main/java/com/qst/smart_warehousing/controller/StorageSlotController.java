package com.qst.smart_warehousing.controller;

import com.qst.smart_warehousing.entity.WmsStorageSlot;
import com.qst.smart_warehousing.service.WmsStorageSlotService;
import com.qst.smart_warehousing.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/storage")
public class StorageSlotController {

    @Autowired
    private WmsStorageSlotService slotService;

    @GetMapping("/slots")
    public Result<List<WmsStorageSlot>> getAllSlots() {
        List<WmsStorageSlot> slots = slotService.list();
        return Result.ok(slots);
    }
}