package com.qst.smartbuildings.config.security;

import com.alibaba.fastjson.JSON;
import com.qst.smartbuildings.config.AuthenticationTokenFilter;
import com.qst.smartbuildings.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


import static com.qst.smartbuildings.common.AuthConst.OPEN_URLS;

@Configuration
@EnableWebSecurity // 开启 Spring Security 核心功能
public class SecurityConfig {

    // 1. 暴露 AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // 2. 密码编码器（必须配置，否则认证会报错）
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 生产环境推荐 BCrypt 加密
    }

    // 3. 自定义的认证过滤器（用于处理 token 认证）
    // !! 改为@component注册，此处废弃
//    @Bean
//    public OncePerRequestFilter authenticationTokenFilter() {
//        return new AuthenticationTokenFilter();
//    }

    // 4. 核心：配置接口权限（开放接口 vs 保护接口）
    @Autowired
    private AuthenticationTokenFilter authenticationTokenFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 关闭 CSRF（跨站请求伪造防护）：前后端分离项目必关（否则 POST/PUT/DELETE 请求会被拦截）
                .csrf(csrf -> csrf.disable())
                // 禁用默认的表单登录
                .formLogin(form -> form.disable())
                // 禁用默认的HTTP基本认证
                .httpBasic(basic -> basic.disable())
                // 禁用默认的登出处理
                .logout(logout -> logout.disable())
                // 配置请求授权规则（顺序很重要：先配“开放接口”，再配“默认保护规则”）
                .authorizeHttpRequests(authorize -> authorize
                        // -------------------------- 开放接口列表 --------------------------
                        // 开放接口：无需登录即可访问
                        .requestMatchers(OPEN_URLS).permitAll()
                        // 所有其他请求：必须登录后才能访问
                        .anyRequest().authenticated()
                )
                // 2. 配置自定义的认证过滤器（在用户名密码认证过滤器之前执行）
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // 3. 配置异常处理（未登录/未授权时的自定义响应）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                ((request, response, authException) -> {
                                    response.setContentType("application/json;charset=UTF-8");
                                    response.getWriter().write(JSON.toJSONString(Result.error(401, "未登录")));
                                    response.getWriter().flush();
                                }
                                ))
                        .accessDeniedHandler(
                                ((request, response, accessDeniedException) -> {
                                    response.setContentType("application/json;charset=UTF-8");
                                    response.getWriter().write(JSON.toJSONString(Result.error(403, "未授权")));
                                    response.getWriter().flush();
                                }
                                ))
                );
        return http.build();
    }
}