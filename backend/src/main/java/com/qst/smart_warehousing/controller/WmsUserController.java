package com.qst.smart_warehousing.controller;


import com.qst.smart_warehousing.DTO.UserAssignRoleDTO;
import com.qst.smart_warehousing.DTO.UserQueryDTO;
import com.qst.smart_warehousing.DTO.WmsUserSaveDTO;
import com.qst.smart_warehousing.entity.Result;
import com.qst.smart_warehousing.service.WmsUserService;
import io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "WMS系统用户管理")
@RestController
@RequestMapping("/wms")
public class WmsUserController {

    @Autowired
    private WmsUserService wmsUserService;

    /** 获取用户列表（支持分页与按用户名/角色筛选） */
    @GetMapping("/user/list")
    public Result<Map<String, Object>> getUserList(@ParameterObject UserQueryDTO queryDTO) {
        Map<String, Object> pageData = wmsUserService.getUserPage(queryDTO);
        return Result.ok(pageData);
    }


    /** 新增系统用户 */
    @PostMapping("/user/add")
    public Result<Void> createUser(@RequestBody WmsUserSaveDTO data) {
        wmsUserService.createUser(data);
        return Result.ok();
    }

    /** 修改系统用户信息 */
    @PutMapping("/user/update")
    public Result<Void> updateUser(@RequestBody WmsUserSaveDTO data) {
        wmsUserService.updateUser(data);
        return Result.ok();
    }

    /** 删除用户 */
    @DeleteMapping("/user/delete/{id}")
    public Result<Void> deleteUser(@PathVariable("id") Long id) {
        wmsUserService.deleteUser(id);
        return Result.ok();
    }

    /** 更改用户启用/禁用状态 */
    @PutMapping("/user/status/{id}")
    public Result<Void> updateUserStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        // 前端通过修改状态传递的值常包含在 body 中或作为参数，这里安全提取 status 状态值
        Integer status = Integer.valueOf(body.get("status").toString());
        wmsUserService.updateUserStatus(id, status);
        return Result.ok();
    }

    /** 获取所有角色列表（用于下拉框选择或角色赋权） */
    @GetMapping("/role/list")
    public Result<List<Map<String, Object>>> getRoleList() {
        return Result.ok(wmsUserService.listAllRoles());
    }

    /** 为用户分配角色 */
    @PostMapping("/user/assign-roles")
    public Result<Void> assignUserRoles(@RequestBody UserAssignRoleDTO data) {
        wmsUserService.assignUserRoles(data.getUserId(), data.getRoleIds());
        return Result.ok();
    }
}