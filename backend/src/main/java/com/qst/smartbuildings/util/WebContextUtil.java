package com.qst.smartbuildings.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class WebContextUtil {
    /**
     * 获取当前请求的 HttpServletResponse 对象
     * @return HttpServletResponse 若当前线程无请求上下文，返回 null
     */
    public static HttpServletResponse getResponse() {
        // 获取当前请求上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null; // 非 web 请求环境（如单元测试）可能为 null
        }
        // 从上下文获取响应对象
        return attributes.getResponse();
    }
    // 可选：同时封装获取请求对象的方法
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}