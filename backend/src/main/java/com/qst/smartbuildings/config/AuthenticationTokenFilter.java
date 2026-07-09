package com.qst.smartbuildings.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.qst.smartbuildings.common.AuthConst;
import com.qst.smartbuildings.entity.AdminUser;

import com.qst.smartbuildings.util.RedisClient;
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

@Component
public class AuthenticationTokenFilter extends OncePerRequestFilter implements Ordered {


    @Autowired
    private RedisClient redisClient;


    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain) throws ServletException, IOException, IOException {
        // 从请求头中获取token
        String token = request.getHeader(AuthConst.TOKEN_NAME);
        logger.info(request.getRequestURI());
        response.setContentType(AuthConst.DEFAULT_CONTENT_TYPE);
        // 如果token为空，直接放行，不为空则根据token从Redis中获取用户信息
        if (StrUtil.isNotEmpty(token)) {
            // 1. 调用单参数get方法，返回Object（可能是LinkedHashMap）
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
        }
        chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return 2000;
    }

}