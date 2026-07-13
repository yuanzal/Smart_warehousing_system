package com.qst.smart_warehousing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smart_warehousing.entity.WmsStorageSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WmsStorageSlotMapper extends BaseMapper<WmsStorageSlot> {
    @Select("SELECT * FROM wms_storage_slot " +
            "WHERE status = 0 AND project_id = #{projectId} AND tenant_id = #{tenantId} " +
            "LIMIT 1")
    WmsStorageSlot selectFirstEmptySlot(@Param("projectId") Integer projectId, @Param("tenantId") Integer tenantId);
}
