package com.qst.smart_warehousing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qst.smart_warehousing.entity.WmsParcel;

public interface WmsParcelService extends IService<WmsParcel> {
    /**
     * 包裹自动/人工入库落位
     * @param barcode 包裹条码
     * @param targetSlotId 目标分配货位ID
     * @return 是否成功
     */
    boolean inbound(String barcode, Long targetSlotId);

    /**
     * 包裹出库下架
     * @param barcode 包裹条码
     * @return 是否成功
     */
    boolean outbound(String barcode);

    /**
     * 库内移位（从A货位移到B货位）
     * @param barcode 包裹条码
     * @param destSlotId 目的新货位ID
     * @return 是否成功
     */
    boolean moveSlot(String barcode, Long destSlotId);
}
