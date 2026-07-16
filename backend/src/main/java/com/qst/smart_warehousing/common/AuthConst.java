package com.qst.smart_warehousing.common;

public class AuthConst {
    //请求中的token名
    public static final String TOKEN_NAME = "Admin-Token";
    //默认的content-type
    public static final String DEFAULT_CONTENT_TYPE = "application/json;charset=UTF-8";
    //    Redis中的token前缀
    public static final String USER_TOKEN_HEAD = "user_token_";
    //    登录过期时间
    public static final Integer TOKEN_EXPIRED_TIME = 60*60*24*7;
    // 放行接口
    public static final String[] OPEN_URLS = new String[]{
            "/login",          // 登录接口
            "/logout",         // 登出接口
            "/relogin",        // 重新登录接口
            "/captcha",        // 验证码接口（如果有）
            "/api/**",
            "/api/public/**",   // 其他公开接口（支持通配符）
            "/ai_image/**", //  ai图片接口
            "/actuator/**",
            // Swagger3 / Springdoc OpenAPI 标准放行路径
            "/swagger-ui/**",          // Swagger UI 网页资源
            "/v3/api-docs/**",         // OpenAPI 3.0 接口描述数据 JSON
            "/swagger-ui.html",        // 旧版兼容入口
            "/swagger-resources/**",   // Swagger 资源配置
            "/webjars/**",             // 网页依赖的静态 jar 包资源
            // 额外放行
            "/wms/sorting-task/agv/report-location",    // 内网安全高频上传接口，直接放行
            "/wms/sorting-task/callback/status",
            "/error"
    };
}
