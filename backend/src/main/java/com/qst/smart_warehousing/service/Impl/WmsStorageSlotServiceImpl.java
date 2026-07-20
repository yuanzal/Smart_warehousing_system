package com.qst.smart_warehousing.service.Impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smart_warehousing.entity.WmsStorageSlot;
import com.qst.smart_warehousing.mapper.WmsStorageSlotMapper;
import com.qst.smart_warehousing.service.WmsStorageSlotService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WmsStorageSlotServiceImpl extends ServiceImpl<WmsStorageSlotMapper, WmsStorageSlot> implements WmsStorageSlotService {
    @Resource
    WmsStorageSlotMapper wmsStorageSlotMapper;

    @Override
    public boolean checkSlotNoParcel(Long slotId) {
        // 查询关联包裹数量，大于0代表有包裹占用，不能删除
        Long bindCount = wmsStorageSlotMapper.countBindParcel(slotId);
        return bindCount == 0;
    }

    @Override
    public void fillParcelWeightInfo(List<WmsStorageSlot> slotList) {
        if (slotList == null || slotList.isEmpty()) {
            return;
        }
        // 提取所有货位ID
        List<Long> slotIdList = slotList.stream()
                .map(WmsStorageSlot::getSlotId)
                .collect(Collectors.toList());

        // 批量联表查询货位+包裹数据
        List<WmsStorageSlot> slotParcelList = wmsStorageSlotMapper.listSlotWithParcel(slotIdList);

        // 转Map：key=slotId，value=带包裹信息的货位
        Map<Long, WmsStorageSlot> slotParcelMap = slotParcelList.stream()
                .collect(Collectors.toMap(WmsStorageSlot::getSlotId, item -> item));

        // 循环给原分页列表填充扩展字段
        for (WmsStorageSlot slot : slotList) {
            WmsStorageSlot parcelInfo = slotParcelMap.get(slot.getSlotId());
            if (parcelInfo != null) {
                slot.setParcelCode(parcelInfo.getParcelCode());
                slot.setCurrentWeight(parcelInfo.getCurrentWeight());
            }
            // 无匹配包裹时 parcelCode / currentWeight 保持null，前端显示"-空闲-"
        }
    }
}