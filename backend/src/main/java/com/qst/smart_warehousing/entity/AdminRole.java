package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_role")
public class AdminRole {
    @TableId(value = "role_id", type = IdType.AUTO)
    private Long roleId;
    private String roleName;
    private LocalDateTime createTime;
}