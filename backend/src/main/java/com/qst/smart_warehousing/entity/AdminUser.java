package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("admin_user")
@Schema(title = "用户表")
public class AdminUser implements Serializable, UserDetails {
    private static final long serialVersionUID = 1L;

    @Schema(title = "主键")
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    @Schema(title = "用户名")
    private String username;

    @Schema(title = "密码")
    private String password;

    @Schema(title = "头像")
    private String img;

    @Schema(title = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(title = "真实姓名")
    private String realname;

    @Schema(title = "手机号")
    private String mobile;

    @Schema(title = "邮箱")
    private String email;

    @Schema(title = "0 未选择 1 男 2 女 ")
    private Integer sex;


    @Schema(title = "岗位")
    private String post;

    @Schema(title = "状态,0禁用,1正常,2未激活")
    private Integer status;

    @Schema(title = "最后登录时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastLoginTime;

    @Schema(title = "最后登录IP")
    private String lastLoginIp;

    @Schema(title = "租户id")
    private Integer tenantId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}

