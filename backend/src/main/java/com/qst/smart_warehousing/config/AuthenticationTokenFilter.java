package com.qst.smart_warehousing.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.qst.smart_warehousing.common.AuthConst;
import com.qst.smart_warehousing.entity.AdminUser;
import com.qst.smart_warehousing.util.RedisClient;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class AuthenticationTokenFilter extends OncePerRequestFilter implements Ordered {

    @Autowired
    private RedisClient redisClient;

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        logger.info("Request URI: " + requestURI);

        // ⚡ 降级策略一：AGV高频位置上报与AGV状态回调属于极核心的物联基础链路，直接绿色通道放行
        if (requestURI.contains("/wms/sorting-task/agv/report-location") ||
                requestURI.contains("/wms/sorting-task/callback/status")) {
            chain.doFilter(request, response);
            return;
        }

        // 从请求头中获取token
        String token = request.getHeader(AuthConst.TOKEN_NAME);
//        logger.info(requestURI+":"+token);
        response.setContentType(AuthConst.DEFAULT_CONTENT_TYPE);

        // 如果token为空，直接放行，不为空则根据token从Redis中获取用户信息
        if (StrUtil.isNotEmpty(token)) {
            try {
                // 1. 正常情况：调用单参数get方法获取缓存
                Object obj = redisClient.get(token);

                // 2. 用JSON工具将Object转为AdminUser
                AdminUser user = null;
                if (obj != null) {
                    // 先转为JSON字符串，再解析为AdminUser
                    user = JSONUtil.toBean(JSONUtil.toJsonStr(obj), AdminUser.class);
                }

                // 3. 如果用户存在，创建认证对象并设置到上下文
                if (user != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // ⚡ 降级策略二：捕捉 Redis 连接/查询超时等严重异常，开启应急安全降级
                System.err.println("【安全网关警报】Redis 连接异常，安全网关开启自验降级防线！错误详情: " + e.getMessage());

                // 4. 宕机自验降级：在 Redis 挂掉的极端情况下，只要前端传了 Token（且格式符合基本规范，比如长度>10）
                // 此时不允许系统崩溃，构建一个临时受限的 AdminUser 用户放入安全上下文，保证页面不会卡死且业务可以临时运转
                if (token.length() > 10) {
                    AdminUser fallbackUser = new AdminUser();
                    fallbackUser.setUsername("fallback_emergency_user"); // 应急临时用户
                    // 如果你的 AdminUser 里有 setAuthorities 或其他角色初始化方法，可以在这里进行简单赋权

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(fallbackUser, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    System.out.println("【安全网关】已成功发放 Redis 宕机保护临时凭证，放行请求。");
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return 2000;
    }
}