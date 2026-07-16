package com.qst.smart_warehousing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smart_warehousing.DTO.UserQueryDTO;
import com.qst.smart_warehousing.entity.AdminUser;
import com.qst.smart_warehousing.entity.BPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    // 💡 核心修复：添加了 @Param("page") 和 @Param("query")
    // 这样动态 SQL 脚本里的 #{query.username} 和 #{query.status} 就能被精准识别了！
    @Select("<script>" +
            "SELECT u.* FROM admin_user u " +
            "<if test='query.roleId != null'>" +
            "  INNER JOIN admin_user_role ur ON u.user_id = ur.user_id AND ur.role_id = #{query.roleId} " +
            "</if>" +
            "<where>" +
            "  <if test='query.username != null and query.username != \"\"'>" +
            "    AND (u.username LIKE CONCAT('%', #{query.username}, '%') OR u.realname LIKE CONCAT('%', #{query.username}, '%'))" +
            "  </if>" +
            "  <if test='query.status != null'>" +
            "    AND u.status = #{query.status}" +
            "  </if>" +
            "</where>" +
            "</script>")
    List<AdminUser> selectUserPage(@Param("page") BPage<AdminUser> page, @Param("query") UserQueryDTO query);
}