package com.qst.smart_warehousing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smart_warehousing.entity.WmsAgvDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WmsAgvDeviceMapper extends BaseMapper<WmsAgvDevice> {

    /**
     * 💡 调度中台核心：动态寻找当前项目/租户下最适合的【待命空闲(status=1)】AGV小车
     */
    @Select("SELECT * FROM wms_agv_device " +
            "WHERE status = 1 AND project_id = #{projectId} AND tenant_id = #{tenantId} " +
            "LIMIT 1")
    WmsAgvDevice selectFirstIdle(@Param("projectId") Integer projectId, @Param("tenantId") Integer tenantId);
}