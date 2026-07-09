package com.qst.smartbuildings.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web配置类 - 固定路径映射（写死配置）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 固定映射规则：将/ai_image/** 映射到项目根目录同级的ai_image文件夹
        // 1. 获取项目根目录（与src同级）
        String projectRoot = System.getProperty("user.dir");

        // 2. 拼接ai_image目录的绝对路径（写死路径规则）
        String aiImageAbsolutePath = projectRoot + File.separator + "ai_image" + File.separator;

        // 3. 注册资源处理器
        registry.addResourceHandler("/ai_image/**")  // 访问路径
                .addResourceLocations("file:" + aiImageAbsolutePath)  // 本地文件路径
                .setCachePeriod(0);  // 开发环境不缓存，生产环境可改为3600等
    }
}
