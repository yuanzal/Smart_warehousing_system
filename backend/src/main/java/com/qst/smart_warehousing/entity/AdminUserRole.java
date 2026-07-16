package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin_user_role")
public class AdminUserRole {
    private Long userId;
    private Long roleId;
}