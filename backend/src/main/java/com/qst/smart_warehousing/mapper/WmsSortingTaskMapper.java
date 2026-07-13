package com.qst.smart_warehousing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smart_warehousing.entity.WmsSortingTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WmsSortingTaskMapper extends BaseMapper<WmsSortingTask> {
    // 继承 BaseMapper 即可获得标准自增插入及按 ID 查询能力
}