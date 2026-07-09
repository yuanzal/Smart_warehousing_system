package com.qst.smartbuildings.util;

import com.qst.smartbuildings.entity.AdminUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class UserUtil {
    /**
     * 获取当前登录用户的ID
     * @return 用户ID，如果未登录则返回null
     */
    public static Long getCurrentUserId() {
        // 从安全上下文获取认证信息
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        // 检查认证信息是否存在且已认证
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        // 获取认证主体（即AuthenticationTokenFilter中设置的AuthorizationUser）
        Object principal = authentication.getPrincipal();
        // 检查主体类型并提取用户ID
        if (principal instanceof AdminUser) {
            return ((AdminUser) principal).getUserId();
        }

        return null;
    }
}