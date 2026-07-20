package com.qst.smart_warehousing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smart_warehousing.entity.WmsStorageSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WmsStorageSlotMapper extends BaseMapper<WmsStorageSlot> {
    @Select("SELECT * FROM wms_storage_slot " +
            "WHERE status = 0 AND project_id = #{projectId} AND tenant_id = #{tenantId} " +
            "LIMIT 1")
    WmsStorageSlot selectFirstEmptySlot(@Param("projectId") Integer projectId, @Param("tenantId") Integer tenantId);


    /**
     * 根据货位ID查询是否存在关联包裹
     * @param slotId 货位主键
     * @return 存在包裹返回1，无返回0
     */
    @Select("SELECT COUNT(1) FROM wms_parcel WHERE current_slot_id = #{slotId}")
    Long countBindParcel(@Param("slotId") Long slotId);


    /**
     * 批量查询货位对应的包裹编码、当前载荷重量
     * @param slotIdList 分页查询出来的所有货位ID集合
     * @return 货位+包裹关联数据
     */
    @Select("SELECT s.slot_id, p.barcode parcel_code, p.weight current_weight " +
            "FROM wms_storage_slot s " +
            "LEFT JOIN wms_parcel p ON s.slot_id = p.current_slot_id " +
            "WHERE s.slot_id IN (#{slotIdList})")
    List<WmsStorageSlot> listSlotWithParcel(@Param("slotIdList") List<Long> slotIdList);
}
