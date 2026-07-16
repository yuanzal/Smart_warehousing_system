package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qst.smart_warehousing.entity.AdminRole;
import com.qst.smart_warehousing.entity.AdminUser;
import com.qst.smart_warehousing.entity.AdminUserRole;
import com.qst.smart_warehousing.entity.BPage;
import com.qst.smart_warehousing.exception.ServiceException;
import com.qst.smart_warehousing.mapper.AdminRoleMapper;
import com.qst.smart_warehousing.mapper.AdminUserMapper;
import com.qst.smart_warehousing.mapper.AdminUserRoleMapper;
import com.qst.smart_warehousing.DTO.UserQueryDTO;
import com.qst.smart_warehousing.DTO.WmsUserSaveDTO;
import com.qst.smart_warehousing.service.WmsUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.qst.smart_warehousing.util.SecurityUtil;

import java.util.*;
import java.util.stream.Collectors;
// 系统用户管理
@Service
public class WmsUserServiceImpl implements WmsUserService {

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private AdminRoleMapper adminRoleMapper;

    @Autowired
    private AdminUserRoleMapper adminUserRoleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> getUserPage(UserQueryDTO queryDTO) {
        Map<String, Object> result = new HashMap<>();
        BPage<AdminUser> bPage = queryDTO.parse();

        List<AdminUser> users = adminUserMapper.selectUserPage(bPage, queryDTO);
        bPage.setRecords(users);

        if (users.isEmpty()) {
            result.put("list", Collections.emptyList());
            result.put("total", 0L);
            return result;
        }

        List<Long> userIds = users.stream().map(AdminUser::getUserId).collect(Collectors.toList());
        List<Map<String, Object>> userRolesRelation = adminRoleMapper.selectRolesByUserIdsList(userIds);

        Map<Long, List<Map<String, Object>>> userRolesMap = new HashMap<>();
        for (Map<String, Object> relation : userRolesRelation) {
            Long userId = Long.valueOf(relation.get("userId").toString());
            Map<String, Object> roleInfo = new HashMap<>();
            roleInfo.put("id", relation.get("id"));
            roleInfo.put("name", relation.get("name"));
            userRolesMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(roleInfo);
        }

        List<Map<String, Object>> responseList = new ArrayList<>();
        for (AdminUser user : users) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", user.getUserId());
            row.put("username", user.getUsername());
            row.put("realName", user.getRealname()); // 正确映射实体中无下划线的 realname
            row.put("email", user.getEmail());
            row.put("status", user.getStatus());
            row.put("createTime", user.getCreateTime());
            row.put("mobile", user.getMobile());
            row.put("tenantId", user.getTenantId());
            row.put("roles", userRolesMap.getOrDefault(user.getUserId(), Collections.emptyList()));
            responseList.add(row);
        }

        result.put("list", responseList);
        result.put("total", bPage.getTotal());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(WmsUserSaveDTO dto) {
        AdminUser user = new AdminUser();
        user.setUsername(dto.getUsername());
        user.setRealname(dto.getRealname()); // 将 DTO 中的驼峰数据精准填入实体的 realname
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setMobile(dto.getMobile());
        // 1. 获取当前登录操作人的信息
        AdminUser adminUser = SecurityUtil.getLoginUser();

        // 2. 💡 分层处理：判断当前登录人是不是“平台超级管理员”
        if (SecurityUtil.isPlatformAdmin()) {

            // 🌟 情况 A：如果他是“平台超级管理员”（比如系统开发商、运维人员，租户ID通常为0）
            // 平台超管拥有至高无上的跨租户特权！他创建用户时，允许通过前端表单传入指定的 tenantId
            if (dto.getTenantId() == null) {
                throw new ServiceException("平台管理员创建用户时，必须指定所属租户ID（tenantId）！");
            }
            user.setTenantId(dto.getTenantId()); // 采用前端传来的目标租户ID

        } else {

            // 🌟 情况 B：如果他是普通的“租户管理员”（比如顺丰物流的管理员，租户ID为 101）
            // 此时他没有任何跨租户权限，系统强行越权保护：他创建的用户必须强制继承他自己的租户ID
            user.setTenantId(adminUser.getTenantId()); // 强行继承 101
        }

        // 3. 执行写入
        adminUserMapper.insert(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(WmsUserSaveDTO dto) {
        AdminUser user = adminUserMapper.selectById(dto.getId());
        if (user != null) {
            user.setRealname(dto.getRealname()); // 将 DTO 中的驼峰数据精准填入实体的 realname
            user.setEmail(dto.getEmail());
            user.setStatus(dto.getStatus());
            if(dto.getTenantId() != null){
                user.setTenantId(dto.getTenantId());
            }
            if (StringUtils.hasText(dto.getPassword())) {
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            adminUserMapper.updateById(user);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        adminUserMapper.deleteById(id);
        adminUserRoleMapper.delete(new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getUserId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long id, Integer status) {
        AdminUser user = new AdminUser();
        user.setUserId(id);
        user.setStatus(status);
        adminUserMapper.updateById(user);
    }

    @Override
    public List<Map<String, Object>> listAllRoles() {
        List<AdminRole> roles = adminRoleMapper.selectList(null);
        return roles.stream().map(role -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", role.getRoleId());
            map.put("name", role.getRoleName());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        adminUserRoleMapper.delete(new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                adminUserRoleMapper.insert(new AdminUserRole(userId, roleId));
            }
        }
    }
}