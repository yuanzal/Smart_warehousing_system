package com.qst.smart_warehousing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qst.smart_warehousing.entity.AdminUser;
import com.qst.smart_warehousing.entity.Result;

public interface AdminUserService extends IService<AdminUser> {
    public Result doLogin(AdminUser User);

    Result logout();
}
