package com.qst.smart_warehousing.service.Impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smart_warehousing.entity.WmsStorageSlot;
import com.qst.smart_warehousing.mapper.WmsStorageSlotMapper;
import com.qst.smart_warehousing.service.WmsStorageSlotService;
import org.springframework.stereotype.Service;

@Service
public class WmsStorageSlotServiceImpl extends ServiceImpl<WmsStorageSlotMapper, WmsStorageSlot> implements WmsStorageSlotService {
}