package com.qst.smart_warehousing.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE) // 设置最高优先级，确保先处理跨域，再走其他过滤器（如Security）
public class MyCorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        response.setHeader("Access-Control-Allow-Origin", "*");
        // 2. 允许携带Cookie
        response.setHeader("Access-Control-Allow-Credentials", "true");
        // 3. 允许的请求方法（包含OPTIONS）
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        // 4. 允许的请求头
        response.setHeader("Access-Control-Allow-Headers", "*");
        // 5. 预请求缓存时间
        response.setHeader("Access-Control-Max-Age", "3600");
        // 6. 直接处理OPTIONS预请求（不用走后续业务逻辑）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK); // 返回200，告诉浏览器允许跨域
            return;
        }

        // 非OPTIONS请求，继续走后续流程（如登录接口逻辑）
        chain.doFilter(req, res);
    }

    // init和destroy方法可省略（默认空实现即可）
    @Override
    public void init(FilterConfig filterConfig) {}
    @Override
    public void destroy() {}
}