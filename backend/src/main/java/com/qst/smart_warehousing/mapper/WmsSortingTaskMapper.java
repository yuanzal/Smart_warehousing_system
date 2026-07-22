package com.qst.smart_warehousing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.smart_warehousing.DTO.SortingTaskQueryDTO;
import com.qst.smart_warehousing.VO.SortingTaskVO;
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
            "<if test='tenantId != null and tenantId != 0'>"+
            "AND t.tenant_id = #{tenantId}"+
            "</if>"+
            "AND t.project_id = #{projectId} " +
            "GROUP BY s.zone_name")
    List<Map<String, Object>> selectZoneTaskCount(@Param("tenantId") Integer tenantId,
                                                  @Param("projectId") Integer projectId);
    /**
     * 联表分页查询分拣任务
     */
    @Select({
            "<script>",
            "SELECT",
            "    t.task_id AS taskId,",
            "    t.task_code AS taskCode,",
            "    t.task_code AS taskCode,",
            "    t.parcel_id AS parcelId,",
            "    t.barcode,",
            "    t.source_station_id AS sourceStationId,",
            "    t.target_slot_id AS targetSlotId,",
            "    s.slot_code AS targetSlotCode,",
            "    t.assigned_agv_id AS assignedAgvId,",
            "    a.agv_code AS assignedAgvCode,",
            "    t.status,",
            "    t.create_time AS createTime,",
            "    t.start_time AS startTime,",
            "    t.end_time AS endTime",
            "FROM wms_sorting_task t",
            "LEFT JOIN wms_storage_slot s ON t.target_slot_id = s.slot_id",
            "LEFT JOIN wms_agv_device a ON t.assigned_agv_id = a.agv_id",
            "WHERE 1=1",
            "<if test='tenantId != null and tenantId != 0'>",
            "AND t.tenant_id = #{tenantId}",
            "</if>",
            "<if test='query.keyword != null and query.keyword != \"\"'>",
            "AND (t.task_code LIKE CONCAT('%', #{query.keyword}, '%') OR t.barcode LIKE CONCAT('%', #{query.keyword}, '%'))",
            "</if>",
            "<if test='query.status != null'>",
            "AND t.status = #{query.status}",
            "</if>",
            "<if test='query.startTime != null'>",
            "AND t.create_time &gt;= #{query.startTime}",
            "</if>",
            "<if test='query.endTime != null'>",
            "AND t.create_time &lt;= #{query.endTime}",
            "</if>",
            "ORDER BY t.create_time DESC",
            "</script>"
    })
    IPage<SortingTaskVO> selectTaskPage(
            Page<SortingTaskVO> page,
            @Param("query") SortingTaskQueryDTO query,
            @Param("tenantId") Integer tenantId
    );
}