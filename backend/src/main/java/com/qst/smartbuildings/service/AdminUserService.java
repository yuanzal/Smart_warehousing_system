package com.qst.smartbuildings.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.entity.Result;

public interface AdminUserService extends IService<AdminUser> {
    public Result doLogin(AdminUser User);

    Result logout();
}
