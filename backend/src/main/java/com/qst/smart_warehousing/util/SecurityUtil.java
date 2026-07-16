package com.qst.smart_warehousing.util; // 换成你项目的工具类包名

import com.qst.smart_warehousing.entity.AdminUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全服务工具类
 */
public class SecurityUtil {

    /**
     * 获取当前登录的完整用户对象 (包含租户ID、角色列表等)
     */
    public static AdminUser getLoginUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AdminUser) {
                return (AdminUser) authentication.getPrincipal();
            }
        } catch (Exception e) {
            throw new RuntimeException("获取用户信息异常", e);
        }
        throw new RuntimeException("当前未登录或登录已过期");
    }

    /**
     * 获取当前登录用户的ID
     */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /**
     * 获取当前登录用户的用户名
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 获取当前登录用户的租户ID
     */
    public static Integer getTenantId() {
        return getLoginUser().getTenantId();
    }

    /**
     * 判断当前用户是否为平台超级管理员
     */
    public static boolean isPlatformAdmin() {
        // 假设租户 ID 为 0 代表全平台超级管理员
        return Integer.valueOf(0).equals(getTenantId());
    }

    /**
     * 顺便封装：生成 BCrypt 密码密文（Spring Security 默认加密）
     */
    public static String encryptPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

}