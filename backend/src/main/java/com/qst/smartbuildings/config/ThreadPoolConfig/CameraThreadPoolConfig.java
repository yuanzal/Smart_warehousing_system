package com.qst.smartbuildings.config.ThreadPoolConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

public class CameraThreadPoolConfig {

    @Bean(name = "CameraProcessTaskExecutor")
    public Executor CameraProcessTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数（默认线程数）
        executor.setCorePoolSize(4);
        // 最大线程数
        executor.setMaxPoolSize(8);
        // 缓冲队列大小
        executor.setQueueCapacity(200);
        // 允许线程空闲时间（单位：秒）
        executor.setKeepAliveSeconds(60);
        // 线程池名前缀
        executor.setThreadNamePrefix("my-cameras-thread-");

        // 线程池对拒绝任务的处理策略
        // CallerRunsPolicy：由调用线程处理该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化
        executor.initialize();
        return executor;
    }
}
