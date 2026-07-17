package com.qst.smart_warehousing.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "表单保存数据对象")
@Data
public class WmsUserSaveDTO {
    private Long id; // 对应前端表单的 row.id / userForm.id
    private String username;
    private String realname;
    private String password;
    private String email;
    private String mobile;
    private Integer status; // 1-启用, 0-禁用
    // 1. 如果是平台超级管理员在创建“企业一号人物”，前端传这个字段，后端采纳。
    // 2. 如果是企业管理员创建员工，前端不传（或传null），后端通过安全上下文自动用当前登录人租户ID覆盖它。
    private Integer tenantId;
}

