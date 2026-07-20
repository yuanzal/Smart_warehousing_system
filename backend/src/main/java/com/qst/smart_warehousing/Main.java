package com.qst.smart_warehousing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//自定义线程池配置
@EnableAsync
//定时器任务
@EnableScheduling

// TODO： 分级鉴权页面展示切换还没实现
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}