package com.qst.smart_warehousing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qst.smart_warehousing.entity.WmsStorageSlot;

import java.util.List;

public interface WmsStorageSlotService extends IService<WmsStorageSlot> {
    /**
     * 判断货位是否未绑定包裹，无包裹才可删除
     */
    boolean checkSlotNoParcel(Long slotId);
    /**
     * 左联包裹表，给货位填充 parcelCode、currentWeight 扩展字段
     */
    void fillParcelWeightInfo(List<WmsStorageSlot> slotList);
}