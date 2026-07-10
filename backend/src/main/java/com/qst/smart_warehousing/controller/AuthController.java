package com.qst.smart_warehousing.controller;

import cn.hutool.core.util.StrUtil;
import com.qst.smart_warehousing.common.AuthorizationCodeEnum;
import com.qst.smart_warehousing.entity.AdminUser;
import com.qst.smart_warehousing.entity.Result;
import com.qst.smart_warehousing.exception.NoLoginException;
import com.qst.smart_warehousing.service.AdminUserService;
import com.qst.smart_warehousing.util.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "用户登录相关接口")
public class AuthController {

    @Autowired
    private AdminUserService adminUserService;
    /**
     * 登录方法
     */
    @PostMapping(value = "/login")
    @Operation(summary = "用户登录")
    public Result doLogin(@Valid @RequestBody AdminUser user, HttpServletResponse response) throws Exception {

        if (StrUtil.trimToNull(user.getUsername()) == null) {
            return Result.error(AuthorizationCodeEnum.AUTHORIZATION_USERNAME_REQUIRED);
        }
        Result result = adminUserService.doLogin(user);

        return result;
    }

    @RequestMapping(value = "/logout")
    @Operation(tags = "用户注销")
    public Result logout() {
        return adminUserService.logout();
    }

    /**
     * 查询当前登录用户
     */
    @PostMapping("/queryLoginUser")
    @Operation(summary = "查询当前登录用户")
    public Result<AdminUser> queryLoginUser() {
        AdminUser user = adminUserService.getById(UserUtil.getCurrentUserId());
        if (user == null) {
            throw new NoLoginException();
        }
        user.setPassword("");
        return Result.ok(user);
    }
}