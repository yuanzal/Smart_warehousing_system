package com.qst.smart_warehousing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smart_warehousing.entity.WmsSortingTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WmsSortingTaskMapper extends BaseMapper<WmsSortingTask> {
    // 继承 BaseMapper 即可获得标准自增插入及按 ID 查询能力

    /**
     * 按库区统计当日完成的分拣任务量
     * @param tenantId 租户ID
     * @param projectId 项目ID
     */
    @Select("SELECT s.zone_name, COUNT(t.task_id) AS task_count " +
            "FROM wms_sorting_task t " +
            "LEFT JOIN wms_storage_slot s ON t.target_slot_id = s.slot_id " +
            "WHERE t.status = 4 " +
            "AND DATE(t.create_time) = CURDATE() " +
            "AND t.tenant_id = #{tenantId} " +
            "AND t.project_id = #{projectId} " +
            "GROUP BY s.zone_name")
    List<Map<String, Object>> selectZoneTaskCount(@Param("tenantId") Integer tenantId,
                                                  @Param("projectId") Integer projectId);
}