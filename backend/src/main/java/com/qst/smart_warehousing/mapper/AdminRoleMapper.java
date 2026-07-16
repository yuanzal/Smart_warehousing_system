package com.qst.smart_warehousing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smart_warehousing.entity.AdminRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRole> {

    @Select("<script>" +
            "SELECT ur.user_id as userId, r.role_id as id, r.role_name as name " +
            "FROM admin_role r " +
            "INNER JOIN admin_user_role ur ON r.role_id = ur.role_id " +
            "WHERE ur.user_id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<String, Object>> selectRolesByUserIdsList(@Param("userIds") List<Long> userIds);
}